from django import forms
from django.contrib.auth.models import User
from django.contrib.auth.forms import UserCreationForm

from ups.models import Package


class UserRegisterForm(UserCreationForm):
    email = forms.EmailField()

    class Meta:
        model = User
        fields = ['username', 'email', 'password1', 'password2']


class UserUpdateForm(forms.ModelForm):
    email = forms.EmailField()

    class Meta:
        model = User
        fields = ['username', 'email']


# User Update Specific Package with ID
class UpdatePackageForm(forms.ModelForm):
    packageID = forms.IntegerField(min_value=0)
    x = forms.IntegerField(min_value=0)
    y = forms.IntegerField(min_value=0)

    class Meta:
        model = Package
        fields = [ 'x', 'y']

