import math

from django.contrib.auth.mixins import LoginRequiredMixin, UserPassesTestMixin
from django.db.models import Q
from django.http import HttpResponse
from django.shortcuts import render
from django.views.generic import DetailView
from django.contrib import messages

from ups.forms import SearchPackageForm, SearchPostalFeeForm
from ups.models import Package

posts = [
    {
        'author': 'CoreyMS',
        'title': 'Blog Post 1',
        'content': 'First post content',
        'date_posted': 'August 27, 2018'
    },
    {
        'author': 'Jane Doe',
        'title': 'Blog Post 2',
        'content': 'Second post content',
        'date_posted': 'August 28, 2018'
    }
]


def home(request):
    context = {
        'posts': posts
    }
    return render(request, 'ups/home.html', context)


def packageSearch(request):
    data = []
    form = SearchPackageForm(request.POST)
    if request.method == 'POST':
        if form.is_valid():
            packageID = form.cleaned_data.get('packageID')
            data = Package.objects.filter(Q(PackageID=packageID))
            print(data)
            if len(data) == 0:
                messages.warning(request, f'Package not found!')

    return render(request, 'ups/packageSearch.html', {'form': form, "data": data})


def SearchPostalFee(request):
    data = []
    timeEst = []
    form = SearchPostalFeeForm(request.POST)
    if request.method == 'POST':
        if form.is_valid():
            x = form.cleaned_data.get('destX')
            y = form.cleaned_data.get('destY')

            initPrice = 2
            distance = math.sqrt(x ^ 2 + y ^ 2)
            data = distance * initPrice

            if data < 20:
                timeEst = 1
            else:
                timeEst = 2

    return render(request, 'ups/packagePrice.html', {'form': form, "data": data, "timeEst": timeEst})
