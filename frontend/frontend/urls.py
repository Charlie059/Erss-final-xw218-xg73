from django.contrib import admin
from django.contrib.auth import views as auth_views
from django.urls import path, include
from users import views as user_views
from ups import views as ups_views

urlpatterns = [
    path('admin/', admin.site.urls),
    path('register/', user_views.register, name='register'),
    path('profile/', user_views.profile, name='profile'),
    path('login/', auth_views.LoginView.as_view(template_name='users/login.html'), name='login'),
    path('logout/', auth_views.LogoutView.as_view(template_name='users/logout.html'), name='logout'),
    path('', include('ups.urls')),
    path('verification/', include('verify_email.urls')),
    path('updatePackage/', user_views.updatePackage, name='package-update'),
    path('searchPostalFee/', ups_views.SearchPostalFee, name='search-postal-fee'),
    path('searchTruck/', ups_views.SearchTruck, name='search-truck')
]
