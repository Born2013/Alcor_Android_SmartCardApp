ccflags-y += -I$(srctree)/drivers/misc/mediatek/alsps/inc
ccflags-y += -I$(srctree)/drivers/misc/mediatek/hwmon/include
obj-y	:=  stk3x1x.o

STK3X1X_DEFINES := $(shell echo $(CONFIG_BIRD_STK3X1X_BIRD_C_PRAMS) | tr a-z A-Z)
STK3X1X_CONFIG_DEFINE := $(foreach n,$(STK3X1X_DEFINES),-D$(n))

$(info $(STK3X1X_DEFINES) )
$(info $(STK3X1X_CONFIG_DEFINE) )

ccflags-y += $(STK3X1X_CONFIG_DEFINE)
