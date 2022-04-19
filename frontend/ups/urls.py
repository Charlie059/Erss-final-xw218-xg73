from django.urls import path

from ups import views

urlpatterns = [
    path('', views.home, name='home'),
    path('search/', views.packageSearch, name='package-search')
]
