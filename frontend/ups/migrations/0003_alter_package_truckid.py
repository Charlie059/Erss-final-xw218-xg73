# Generated by Django 4.0.4 on 2022-04-20 21:10

from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):

    dependencies = [
        ('ups', '0002_alter_package_packageid_alter_truck_truckid'),
    ]

    operations = [
        migrations.AlterField(
            model_name='package',
            name='TruckID',
            field=models.ForeignKey(null=True, on_delete=django.db.models.deletion.CASCADE, related_name='truck_packages', to='ups.truck'),
        ),
    ]
