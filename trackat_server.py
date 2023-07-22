from flask import Flask, request
from functools import wraps
import jwt
from twilio import twiml
import json
import datetime

import TK_database as database


app = Flask(__name__)

app.config['SECRET_KEY'] = 'kat'


# def token_required(f):
#     @wraps(f)
#     def decorated(*args,**kwargs):
#         token = request.args.get('token')

@app.route("/", methods=['GET'])
def home():
    return "<h1>TRACKAT API</h1>"
        
@app.route('/receive_data',methods=['GET','POST'])
def sms():
    # number = request.form['FROM']

    if request.form:
        body = request.form["Body"]
        print(body)
        info = json.loads(body)
        save_location(info)

        return 200
   

    return 400

@app.route('/locations', methods=['GET', 'POST'])
def locations():
    '''
    Get the list of all locations
    '''

    locations = database.get_locations(0)
    locations_response = {}

    for i in range(locations.count()):
        locations_response[str(i+1)] = {"date" : locations[i].time, "latitude" : locations[i].lattitude, "longitude" : locations[i].longitude}


    return locations_response

def save_location(info):
    '''
    Save location to the database
    '''

    database.add_location(info["id"],info["lat"],info["long"], datetime.datetime.now())

@app.route('/geofences', methods=['GET', 'POST'])
def geofences():
    '''
    Get the list of all geofences or create a new geofence
    '''

    if request.method == 'POST':
        data = request.get_json()
        name = data.get('name')
        points = data.get('points')
        profile_id = data.get('profile_id')

        if not name or not points or not profile_id:
            return jsonify({'message': 'Invalid input data'}), 400

        # Convert points to a JSON string before saving to the database
        points_json = json.dumps(points)

        geofence = Geofence(name=name, points=points_json, profile_id=profile_id)
        database.session.add(geofence)
        database.session.commit()

        return jsonify({'message': 'Geofence created successfully'}), 201

    elif request.method == 'GET':
        geofences = database.session.query(Geofence).all()
        geofences_response = []

        for geofence in geofences:
            # Convert the points JSON string back to a list
            points = json.loads(geofence.points)
            geofences_response.append({
                'id': geofence.geofence_id,
                'name': geofence.name,
                'points': points,
                'profile_id': geofence.profile_id
            })

        return jsonify(geofences_response)

@app.route('/geofences/<int:geofence_id>', methods=['GET'])
def get_geofence(geofence_id):
    '''
    Get a specific geofence by its ID
    '''

    geofence = database.session.query(Geofence).filter_by(geofence_id=geofence_id).first()

    if not geofence:
        return jsonify({'message': 'Geofence not found'}), 404

    # Convert the points JSON string back to a list
    points = json.loads(geofence.points)
    geofence_response = {
        'id': geofence.geofence_id,
        'name': geofence.name,
        'points': points,
        'profile_id': geofence.profile_id
    }

    return jsonify(geofence_response)
    

if __name__ == "__main__":
    app.run(debug=True)