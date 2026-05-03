import json
import requests
import sys

CYPHER_FILE = 'services/circleguard-promotion-service/src/main/resources/seed_faculties.cypher'
NEO4J_URL = 'http://localhost:7474/db/neo4j/tx/commit'
AUTH = ('neo4j', 'password')

def run_cypher():
    try:
        with open(CYPHER_FILE, 'r') as f:
            cypher = f.read()
        
        payload = {
            "statements": [
                {
                    "statement": cypher
                }
            ]
        }
        
        response = requests.post(NEO4J_URL, auth=AUTH, json=payload)
        response.raise_for_status()
        
        result = response.json()
        if result.get('errors'):
            print(f"Errors: {result['errors']}")
            sys.exit(1)
        else:
            print("Successfully seeded Neo4j data.")
            
    except Exception as e:
        print(f"Failed to run cypher: {e}")
        sys.exit(1)

if __name__ == "__main__":
    run_cypher()
