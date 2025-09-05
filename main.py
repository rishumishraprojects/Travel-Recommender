import os
import google.generativeai as genai
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
import googlemaps
from dotenv import load_dotenv

# --- Load environment variables ---
load_dotenv()
GOOGLE_MAPS_API_KEY = os.getenv("GOOGLE_MAPS_API_KEY")
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")

if not GOOGLE_MAPS_API_KEY or not GEMINI_API_KEY:
    raise RuntimeError("API Keys not found in .env file.")

# --- Initialize Clients ---
gmaps = googlemaps.Client(key=GOOGLE_MAPS_API_KEY)
genai.configure(api_key=GEMINI_API_KEY)
gemini_model = genai.GenerativeModel('gemini-2.0-flash')

app = FastAPI(title="Nearby Tourist Locations API")

# --- Data Models ---
class LocationRequest(BaseModel):
    latitude: float = Field(..., example=28.6129)
    longitude: float = Field(..., example=77.2295)
    radius: int = Field(5000, gt=0)

# MODIFIED: Added place_id
class TouristLocation(BaseModel):
    name: str
    place_id: str
    latitude: float
    longitude: float
    rating: float | None = None
    image_url: str | None = None

# NEW: Models for the details endpoint
class PlaceDetailRequest(BaseModel):
    place_id: str
    place_name: str

class PlaceDetailResponse(BaseModel):
    history: str

# --- API Endpoints ---
@app.get("/")
def read_root():
    return {"message": "Welcome! The Tourist Locations API is running."}

@app.post("/tourist-locations/", response_model=list[TouristLocation])
def get_nearby_tourist_locations(location: LocationRequest):
    try:
        places_result = gmaps.places_nearby(
            location=(location.latitude, location.longitude),
            radius=location.radius,
            type='tourist_attraction'
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Google Places API error: {str(e)}")

    locations = []
    for place in places_result.get('results', []):
        image_url = None
        if place.get('photos'):
            photo_reference = place['photos'][0].get('photo_reference')
            if photo_reference:
                image_url = f"https://maps.googleapis.com/maps/api/place/photo?maxwidth=400&photoreference={photo_reference}&key={GOOGLE_MAPS_API_KEY}"
        
        # MODIFIED: Extract and include the place_id
        if place.get('name') and place.get('geometry') and place.get('place_id'):
            locations.append(
                TouristLocation(
                    name=place.get('name'),
                    place_id=place.get('place_id'),
                    latitude=place['geometry']['location']['lat'],
                    longitude=place['geometry']['location']['lng'],
                    rating=place.get('rating'),
                    image_url=image_url
                )
            )
    return locations

# NEW: Endpoint to get history for a specific place
@app.post("/place-details/", response_model=PlaceDetailResponse)
def get_place_history(detail_request: PlaceDetailRequest):
    """
    Generates a brief history of a place using the Gemini API.
    """
    try:
        # We add "Prayagraj, India" to the prompt for better, more specific context
        prompt = (
            f"Provide a brief, engaging history of the tourist attraction '{detail_request.place_name}' "
            f"located in Prayagraj, India. Focus on its significance and key historical events. "
            f"Write it as a single, well-written paragraph suitable for a travel app."
        )
        response = gemini_model.generate_content(prompt)
        return PlaceDetailResponse(history=response.text)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error generating history: {str(e)}")