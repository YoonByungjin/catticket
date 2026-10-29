from django.db import models

class Concert(models.Model):
    title = models.CharField(max_length=255)
    location = models.CharField(max_length=255)
    date_start = models.DateField()
    date_end = models.DateField()
    lat = models.FloatField()
    lng = models.FloatField()
    image_url = models.URLField(blank=True, null=True)
    ticket_url = models.URLField(blank=True, null=True)


    def __str__(self):
        return self.title

class Locker(models.Model):
    station_name = models.CharField(max_length=100)  # 역 이름
    lat = models.FloatField()
    lng = models.FloatField()
    address = models.CharField(max_length=255)
    detail_location = models.TextField()

    def __str__(self):
        return f"{self.station_name} - {self.detail_location}"
    
class Musical(models.Model):
    title = models.CharField(max_length=255)
    location = models.CharField(max_length=255)
    date_start = models.DateField()
    date_end = models.DateField()
    image_url = models.URLField(blank=True, null=True)
    ticket_url = models.URLField(blank=True, null=True)
    lat = models.FloatField()
    lng = models.FloatField()


    def __str__(self):
        return self.title
    
class Exhibition(models.Model):
    title = models.CharField(max_length=255)
    location = models.CharField(max_length=255)
    date_start = models.DateField()
    date_end = models.DateField()
    image_url = models.URLField(blank=True, null=True)
    ticket_url = models.URLField(blank=True, null=True)
    lat = models.FloatField()
    lng = models.FloatField()


    def __str__(self):
        return self.title