from django.db import models
from django.contrib.auth.models import User

# Create AWarehouseDB
from django.utils.timezone import now


class AWareHouse(models.Model):
    WareHouseID = models.BigIntegerField(primary_key=True)  # required int64 WareHouseID
    x = models.BigIntegerField()  # required int64 x
    y = models.BigIntegerField()  # required int64 y

    def __str__(self):
        return "<" + str(self.WareHouseID) + str(self.x) + ", " + str(self.y) + ">"


# Create Ticket from Amazon
# Note: we do not want to delete any ticket, just update status
class Ticket(models.Model):
    PickupWareHouseID = models.ForeignKey(AWareHouse, on_delete=models.CASCADE)


# Create Package
class Package(models.Model):
    PackageID = models.BigIntegerField(primary_key=True)  # required int64 PackageID
    Owner = models.ForeignKey(User, on_delete=models.CASCADE)  # ForeignKey User
    x = models.BigIntegerField()  # required int64 x
    y = models.BigIntegerField()  # required int64 y

    # Package status:
    # 1. Processing: UPS receive pickup request from Amazon but do not send any truck
    # 2. Pickup: UPS send truck to warehouse
    # 3. Delivering: Truck leave warehouse and start to deliver
    # 4. Delivered: Truck has sent package to the user
    status_choice = [
        ('PROC', 'Processing'),
        ('PICK', 'Pickup'),
        ('DELI', 'Delivering'),
        ('DELD', 'Delivered'),
    ]
    Status = models.CharField(max_length=100, default="PROC", choices=status_choice)
    CreateTime = models.DateTimeField(default=now)  # required DateTimeField
    UpdateTime = models.DateTimeField(default=now)  # required DateTimeField

    # One Ticket has many packages
    TicketID = models.ForeignKey(Ticket, related_name='ticket_packages', on_delete=models.CASCADE)  # Ticket.packages
    # One Truck has many packages
    TruckID = models.ForeignKey(Ticket, related_name='truck_packages', on_delete=models.CASCADE)  # Truck.packages


# Create Truck
class Truck(models.Model):
    TruckID = models.BigIntegerField(primary_key=True)  # required int64 TruckID
    x = models.BigIntegerField()  # required int64 x
    y = models.BigIntegerField()  # required int64 y

    # Truck status:
    # 1. Idle: Finish all CMD
    # 2. Traveling: Traveling to warehouse
    # 3. ArriveWarehouse: Finish loading or Arrived warehouse
    # 4. Loading: Loading goods from warehouse
    # 5. Delivering: Truck is sending packages
    status_choice = [
        ('IDLE', 'Idle'),
        ('TRAVELING', 'Traveling'),
        ('ARRIVEWH', 'ArriveWarehouse'),
        ('LOADING', 'Loading'),
        ('DELIVERING', 'Delivering'),
    ]
    Status = models.CharField(max_length=100, default="IDLE", choices=status_choice)


# Create UserChangeDstRequest: When user want to change package destination request
class UserChangeDstRequest(models.Model):
    # User request to change one package destination
    PackageID = models.OneToOneField(Package, on_delete=models.CASCADE)
    x = models.BigIntegerField()  # required int64 x
    y = models.BigIntegerField()  # required int64 y
