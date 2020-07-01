ccflags-y += -I$(srctree)/drivers/input/touchscreen/mediatek/msg2133_update/
ccflags-y += -I$(srctree)/drivers/input/touchscreen/mediatek/
ccflags-y += -I$(srctree)/drivers/misc/mediatek/include/mt-plat/
ccflags-y += -I$(srctree)/drivers/misc/mediatek/include/mt-plat/$(MTK_PLATFORM)/include/

obj-y += mstar_drv_common.o
obj-y += mstar_drv_fw_control.o
obj-y += mstar_drv_hotknot.o
obj-y += mstar_drv_hotknot_queue.o
obj-y += mstar_drv_ic_fw_porting_layer.o
obj-y += mstar_drv_jni_interface.o
obj-y += mstar_drv_main.o
obj-y += mstar_drv_mp_test.o
obj-y += mstar_drv_mtk.o
obj-y += mstar_drv_platform_interface.o
obj-y += mstar_drv_platform_porting_layer.o
obj-y += mstar_drv_utility_adaption.o




CTP_DEFINES := $(shell echo $(CONFIG_BIRD_MSG2XXX_CTP_TSFW) | tr a-z A-Z)
CTP_CONFIG_DEFINE := $(foreach n,$(CTP_DEFINES),-D$(n))

$(info $(CTP_DEFINES) )
$(info $(CTP_CONFIG_DEFINE) )

ccflags-y += $(CTP_CONFIG_DEFINE)
