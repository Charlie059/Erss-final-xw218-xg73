from django.contrib import messages
from django.contrib.auth.decorators import login_required
from django.db.models import Q
from django.shortcuts import render, redirect

from ups.models import Package, Item
from .forms import UserRegisterForm, UserUpdateForm, UpdatePackageForm
from verify_email.email_handler import send_verification_email


def register(request):
    if request.method == 'POST':
        form = UserRegisterForm(request.POST)
        if form.is_valid():
            inactive_user = send_verification_email(request, form)

            messages.success(request, f'Enter verification code from email to activate!')
            return redirect('login')
    else:
        form = UserRegisterForm()
    return render(request, 'users/register.html', {'form': form})


@login_required
def profile(request):
    if request.method == 'POST':
        u_form = UserUpdateForm(request.POST, instance=request.user)

        if u_form.is_valid():
            u_form.save()
            messages.success(request, f'Your account has been updated!')
            return redirect('profile')
    else:
        u_form = UserUpdateForm(instance=request.user)

    packages = Package.objects.filter(Q(EmailAddress=request.user.email))

    context = {
        'u_form': u_form,
        'packages': packages
    }

    return render(request, 'users/profile.html', context)


@login_required
def updatePackage(request):
    data = []
    form = UpdatePackageForm(request.POST)
    if request.method == 'POST':
        if form.is_valid():
            packageID = form.cleaned_data.get('packageID')
            x = form.cleaned_data.get('x')
            y = form.cleaned_data.get('y')
            data = Package.objects.filter(Q(PackageID=packageID) & Q(EmailAddress=request.user.email))
            if len(data) is 1:
                if data[0].Status != "DELI" and data[0].Status != "DELD":
                    package = data[0]
                    package.x = x
                    package.y = y
                    package.save()
                    messages.success(request, f'Update Address of Package Success!')
                else:
                    messages.warning(request, f'Update Address of Package Failure!')

    return render(request, 'users/packageUpdate.html', {'form': form, "data": data})

