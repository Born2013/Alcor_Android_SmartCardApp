# Linux driver folder
ccflags-y += -I$(srctree)/drivers/input/touchscreen/mediatek/GT9XX_hotknot_scp/
ccflags-y += -I$(srctree)/drivers/input/touchscreen/mediatek/
ccflags-y += -I$(srctree)/drivers/misc/mediatek/include/mt-plat/
ccflags-y += -I$(srctree)/drivers/misc/mediatek/include/mt-plat/$(MTK_PLATFORM)/include/
ccflags-y += -I$(srctree)/drivers/misc/mediatek/alsps/stk3x1x_bird_c/
obj-y	+=  gsl_ts_driver.o

CTP_DEFINES := $(shell echo $(CONFIG_BIRD_GSLCTP_TSFW) | tr a-z A-Z)
CTP_CONFIG_DEFINE := $(foreach n,$(CTP_DEFINES),-D$(n))

$(info $(CTP_DEFINES) )
$(info $(CTP_CONFIG_DEFINE) )

ccflags-y += $(CTP_CONFIG_DEFINE)
