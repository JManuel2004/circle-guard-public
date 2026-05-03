import requests
import urllib.parse

DEPT = 'Faculty of Health Sciences (Ciencias de la Salud)'
URL = f'http://localhost:8088/api/v1/health-status/stats/department/{urllib.parse.quote(DEPT)}'

try:
    response = requests.get(URL)
    print(f"Status: {response.status_code}")
    print(f"Body: {response.text}")
except Exception as e:
    print(f"Error: {e}")
