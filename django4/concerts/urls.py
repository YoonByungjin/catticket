# concerts/urls.py
from django.urls import path
from . import views

urlpatterns = [
    path("map/", views.concert_map, name="concert_map"),
    path("map/naver", views.concert_map, name="concert_map"),
    path('api/nearby-lockers/', views.get_nearby_lockers, name='get_nearby_lockers'),
]
