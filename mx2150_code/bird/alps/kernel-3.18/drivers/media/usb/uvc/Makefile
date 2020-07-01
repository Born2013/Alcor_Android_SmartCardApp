uvcvideo-objs  := uvc_driver.o uvc_queue.o uvc_v4l2.o uvc_video.o uvc_ctrl.o \
		  uvc_status.o uvc_isight.o uvc_debugfs.o
ifeq ($(CONFIG_MEDIA_CONTROLLER),y)
uvcvideo-objs  += uvc_entity.o
endif
obj-$(CONFIG_USB_VIDEO_CLASS) += uvcvideo.o
#[BIRD]add by wuchuang for usb camera 20170725 begin
obj-$(CONFIG_BIRD_USB_UVC_CAMERA) += uvcvideo.o
#[BIRD]add by wuchuang for usb camera 20170725 end
