from django import forms


# User Search Specific Package with ID
class SearchPackageForm(forms.Form):
    packageID = forms.IntegerField(min_value=0)
