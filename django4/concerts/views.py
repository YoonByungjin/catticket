# concerts/views.py (수정)
from django.shortcuts import render
from django.http import JsonResponse
from django.views.decorators.csrf import csrf_exempt
from math import radians, cos, sin, asin, sqrt
from .models import Concert, Locker, Musical, Exhibition
from django.core.serializers import serialize
import json

def concert_map(request):
    concerts = Concert.objects.all()
    musicals = Musical.objects.all()
    exhibitions = Exhibition.objects.all()
    concert_list = [
        {
            "title": c.title,
            "location": c.location,
            "date_start": c.date_start.strftime("%Y-%m-%d"),
            "date_end": c.date_end.strftime("%Y-%m-%d"),
            "lat": c.lat,
            "lng": c.lng,
            "image_url":c.image_url,
            "ticket_url":c.ticket_url
        }
        for c in concerts if c.lat and c.lng
    ]
    musical_list = [
        {
            "title": m.title,
            "location": m.location,
            "date_start": m.date_start.strftime("%Y-%m-%d"),
            "date_end": m.date_end.strftime("%Y-%m-%d"),
            "lat": m.lat,
            "lng": m.lng,
            "image_url":m.image_url,
            "ticket_url":m.ticket_url
        }
        for m in musicals if m.lat and m.lng
    ]
    
    exhibition_list = [
        {
            "title": e.title,
            "location": e.location,
            "date_start": e.date_start.strftime("%Y-%m-%d"),
            "date_end": e.date_end.strftime("%Y-%m-%d"),
            "lat": e.lat,
            "lng": e.lng,
            "image_url":e.image_url,
            "ticket_url":e.ticket_url
        }
        for e in exhibitions if e.lat and e.lng
    ]
    if request.path == '/concerts/map/naver':
        return render(request, 'concerts/concert_map_naver.html', {"concerts": json.dumps(concert_list)})
    return render(request, "concerts/concert_map.html", {"concerts": json.dumps(concert_list), "musicals": json.dumps(musical_list), "exhibitions": json.dumps(exhibition_list)})

def haversine(lat1, lon1, lat2, lon2):
    R = 6371000
    lat1, lon1, lat2, lon2 = map(radians, [lat1, lon1, lat2, lon2])
    dlat = lat2 - lat1
    dlon = lon2 - lon1
    
    a = sin(dlat / 2)**2 + cos(lat1) * cos(lat2) * sin(dlon / 2)**2
    c = 2 * asin(sqrt(a))
    return R * c

@csrf_exempt
def get_nearby_lockers(request):
    if request.method == 'POST':
        import json
        data = json.loads(request.body)
        lat = float(data.get('lat'))
        lng = float(data.get('lng'))
        
        lockers = Locker.objects.all()
        nearby = []
        
        for locker in lockers:
            distance = haversine(lat, lng, locker.lat, locker.lng)
            if distance <= 500:
                nearby.append({
                    'station_name' : locker.station_name,
                    'lat' : locker.lat,
                    'lng' : locker.lng,
                    'address' : locker.address,
                    'detail_location' : locker.detail_location,
                })
        return JsonResponse({'lockers' : nearby})
    return JsonResponse({'error' : 'Invalid request'}, status = 400)