from django import forms


# User Search Specific Package with ID
class SearchPackageForm(forms.Form):
    packageID = forms.IntegerField(min_value=0)


# User search Postal Fee
class SearchPostalFeeForm(forms.Form):
    destX = forms.IntegerField(min_value=0)
    destY = forms.IntegerField(min_value=0)
