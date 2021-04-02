import threading, sqlite3, base64, time, random, string, hashlib
from typing import Dict
from google.cloud import aiplatform
from google.protobuf import json_format
from google.protobuf.struct_pb2 import Value
from flask import Flask, request, Response
from concurrent.futures import ThreadPoolExecutor


app = Flask(__name__)
executor = ThreadPoolExecutor(5)
threadLock = threading.Lock()
buffer = {}


@app.route('/m2m', methods=['PUT'])
def m2m():
    notification = request.get_json().get("notifications")[0]
    executor.submit(predictAndInsert, 
        notification['ep'], 
        notification['path'], 
        base64.b64decode(notification['payload']), 
        int(notification['timestamp']))
    return 'Cheers!'


@app.route('/register', methods=['PUT'])
def register():
    account = request.get_json().get("account")
    passwd = request.get_json().get("passwd")
    device_id = request.get_json().get("device_id")
    conn = sqlite3.connect('devices.db')
    cursor = conn.cursor()
    cursor.execute("select * from accounts where id='%s'"%(account))
    if cursor.fetchone() == None:
        salt = ''.join(random.sample(string.ascii_letters + string.digits, 8))
        temp = passwd+salt
        params = (account, 
            salt, 
            str(hashlib.sha256(temp.encode('utf-8')).hexdigest()), 
            device_id)
        cursor.execute("insert into accounts values(?,?,?,?)", params)
        conn.commit()
        conn.close()
        return 'Cheers!'
    else:
        conn.close()
        return Response(status=406)


@app.route('/mobilelogin', methods=['PUT'])
def login():
    account = request.get_json().get("account")
    passwd = request.get_json().get("passwd")
    conn = sqlite3.connect('devices.db')
    cursor = conn.cursor()
    cursor.execute("select * from accounts where id='%s'"%(account))
    if cursor.fetchone() != None:
        cursor.execute("select salt, hashvalue, device_id from accounts where id='%s'"%(account))
        for row in cursor:
            temp = passwd+row[0]
            print(row[1])
            print(str(hashlib.sha256(temp.encode('utf-8')).hexdigest()))
            if str(hashlib.sha256(temp.encode('utf-8')).hexdigest()) != row[1]:
                conn.close()
                return Response(status=406)
            else:
                conn.close()
                return row[2], 200
    else:
        conn.close()
        return Response(status=406)


@app.route('/mobile', methods=['PUT'])
def mobile():
    device_id = request.get_json().get("device_id")
    from_time = request.get_json().get("from_time")
    conn = sqlite3.connect('devices.db')
    cursor = conn.cursor()
    cursor.execute("select time,movement from raw_movements where device_id='%s' and time>%s"%(device_id, from_time))
    data = []
    for row in cursor:
        data.append({'timestamp':row[0],'type':row[1]})
    conn.close()
    return {'data':data}


@app.route('/mobilelatest', methods=['PUT'])
def mobilelatest():
    device_id = request.get_json().get("device_id")
    print(device_id)
    from_time = request.get_json().get("from_time")
    print(from_time)
    conn = sqlite3.connect('devices.db')
    cursor = conn.cursor()
    cursor.execute("select time,movement from raw_movements where device_id='%s' and time>%s order by time desc"%(device_id, from_time))
    data = {}
    for row in cursor:
        data['timestamp'] = row[0]
        data['type'] = row[1]
        conn.close()
        return data


def predictAndInsert(device_id, path, payload, time):
    threadLock.acquire()
    buffer[formatPath(path)] = bytes.decode(payload)
    if len(buffer) == 6:
        buffer['wrist'] = "1"
        print(buffer)
        movement = predict_tabular_classification(buffer)
        conn = sqlite3.connect('devices.db')
        cursor = conn.cursor()
        params = (device_id, time, 1 if float(movement['scores'][0])>0.5 else 0)
        print(params)
        cursor.execute("insert into raw_movements (device_id, time, movement) values (?, ?, ?)", params)
        conn.commit()
        conn.close()
        buffer.clear()
    threadLock.release()
    return


def predict_tabular_classification(
    instance_dict: Dict,
    project: str = "313107887",
    endpoint_id: str = "2207080476760342528",
    location: str = "europe-west4",
    api_endpoint: str = "europe-west4-aiplatform.googleapis.com",
):
    # The AI Platform services require regional API endpoints.
    client_options = {"api_endpoint": api_endpoint}
    # Initialize client that will be used to create and send requests.
    # This client only needs to be created once, and can be reused for multiple requests.
    client = aiplatform.gapic.PredictionServiceClient(client_options=client_options)
    # for more info on the instance schema, please use get_model_sample.py
    # and look at the yaml found in instance_schema_uri
    instance = json_format.ParseDict(instance_dict, Value())
    instances = [instance]
    parameters_dict = {}
    parameters = json_format.ParseDict(parameters_dict, Value())
    endpoint = client.endpoint_path(
        project=project, location=location, endpoint=endpoint_id
    )
    print("p1")
    response = client.predict(
        endpoint=endpoint, instances=instances, parameters=parameters
    )
    print("response")
    print(" deployed_model_id:", response.deployed_model_id)
    # See gs://google-cloud-aiplatform/schema/predict/prediction/tables_classification.yaml for the format of the predictions.
    predictions = response.predictions
    for prediction in predictions:
        return dict(prediction)
    

def formatPath(input):
    if input == '/3000/1/0':
        return "acceleration_x"
    elif input == '/3000/1/1':
        return "acceleration_y"
    elif input == '/3000/1/2':
        return "acceleration_z"
    elif input == '/3000/2/0':
        return "gyro_x"
    elif input == '/3000/2/1':
        return "gyro_y"
    elif input == '/3000/2/2':
        return "gyro_z"


if __name__ =="__main__":
    conn = sqlite3.connect('devices.db')
    cursor = conn.cursor()
    cursor.execute('''create table if not exists 
        accounts (id text primary key, 
        salt text, 
        hashvalue text, 
        device_id text)''')
    cursor.execute('''create table if not exists 
        raw_movements (device_id text, 
        time int, 
        movement int)''')
    conn.commit()
    conn.close()
    app.run(debug=True, port=80, host='0.0.0.0')

