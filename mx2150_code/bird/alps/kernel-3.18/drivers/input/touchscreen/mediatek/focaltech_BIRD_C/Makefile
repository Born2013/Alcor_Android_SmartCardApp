
# Linux driver folder
ccflags-y += -I$(srctree)/drivers/input/touchscreen/mediatek/focaltech_BIRD_C/include/
ccflags-y += -I$(srctree)/drivers/input/touchscreen/mediatek/
ccflags-y += -I$(srctree)/drivers/input/touchscreen/mediatek/focaltech_BIRD_C/include/$(CONFIG_FT5X0X_FIRMWARE)/
ccflags-y += -I$(srctree)/drivers/misc/mediatek/include/mt-plat/
ccflags-y += -I$(srctree)/drivers/misc/mediatek/include/mt-plat/$(MTK_PLATFORM)/include/

#obj-y	+=  lib/
obj-y	+=  focaltech_ctl.o
obj-y	+=  focaltech_ex_fun.o
obj-y	+=  focaltech_flash.o
obj-y	+=  focaltech_Gesture.o
obj-y	+=  focaltech_core.o

CTP_DEFINES := $(shell echo $(CONFIG_BIRD_FOCALTECH_PARAM) | tr a-z A-Z)
CTP_CONFIG_DEFINE := $(foreach n,$(CTP_DEFINES),-D$(n))

$(info $(CTP_DEFINES) )
$(info $(CTP_CONFIG_DEFINE) )

ccflags-y += $(CTP_CONFIG_DEFINE)


