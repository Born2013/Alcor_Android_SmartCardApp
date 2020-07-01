#
# Makefile for the kernel multimedia device drivers.
#

subdir-ccflags-y += -Werror

media-objs	:= media-device.o media-devnode.o media-entity.o

#
# I2C drivers should come before other drivers, otherwise they'll fail
# when compiled as builtin drivers
#
obj-y += i2c/ tuners/
obj-$(CONFIG_DVB_CORE)  += dvb-frontends/

#
# Now, let's link-in the media core
#
ifeq ($(CONFIG_MEDIA_CONTROLLER),y)
  obj-$(CONFIG_MEDIA_SUPPORT) += media.o
endif
obj-$(CONFIG_VIDEO_DEV) += v4l2-core/
#[BIRD]add by wuchuang for usb camera 20170725 begin
obj-$(CONFIG_BIRD_USB_UVC_CAMERA) += v4l2-core/
#[BIRD]add by wuchuang for usb camera 20170725 end
obj-$(CONFIG_DVB_CORE)  += dvb-core/

# There are both core and drivers at RC subtree - merge before drivers
obj-y += rc/

#
# Finally, merge the drivers that require the core
#

obj-y += common/ platform/ pci/ usb/ mmc/ firewire/ parport/
obj-$(CONFIG_VIDEO_DEV) += radio/

