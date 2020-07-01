

ccflags-y +=-I$(srctree)/drivers/spi/mediatek/mt6797/

MODFLAGS       =$(MODFLAGS)  -DMODULE -fno-pic  

sunwave-objs := sunwavecorp.o   platform.o misce.o

#ifdef CONFIG_SUNWAVECORP_MT6797
sunwave-objs +=  mt6797_dts.o
obj-y= sunwave.o mt6797_dts_plat.o
#endif

