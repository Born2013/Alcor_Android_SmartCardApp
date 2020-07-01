/* Copyright (C) MicroArray
 * MicroArray Fprint Driver Code
 * mtk-settings.c
 * Date: 2016-11-17
 * Version: v4.0.03
 * Author: guq
 * Contact: guq@microarray.com.cn
 */

#include "mtk-settings.h"

static int ret;
//pin control sturct data, define using for the dts settings,
struct pinctrl *mas_finger_pinctrl;
struct pinctrl_state 		*mas_finger_power2v8_on, *mas_finger_power2v8_off, 	//power2v8
							*mas_finger_power1v8_on, *mas_finger_power1v8_off,	//power1v8
							*mas_finger_eint_on, *mas_finger_eint_off,			//eint
							*mas_spi_ck_on, *mas_spi_ck_off,					//for ck
							*mas_spi_cs_on, *mas_spi_cs_off,					//for cs
							*mas_spi_mi_on, *mas_spi_mi_off,					//for mi
							*mas_spi_mo_on, *mas_spi_mo_off,					//for mo
							*mas_spi_default;									//same odms only use default to setting the dts



/**
 *    the platform struct start,for getting the platform device to set gpio state
 */
//#ifdef CONFIG_OF
static struct of_device_id sof_match[] = {
        { .compatible = MA_DTS_NAME, },						//this name is used for matching the dts device for settings the gpios
};
MODULE_DEVICE_TABLE(of, sof_match);
//#endif
static struct platform_driver spdrv = {
        .probe    = mas_plat_probe,
        .remove  = mas_plat_remove,
        .driver = {
                .name  = MA_DRV_NAME,
                .owner = THIS_MODULE,                   
//#ifdef CONFIG_OF
                .of_match_table = sof_match,
//#endif
        }
};
/**
  *  the platform struct start,for getting the platform device to set gpio state end
  */


/**
 *  the spi struct date start,for getting the spi_device to set the spi clock enable start
 */

struct spi_device_id sdev_id = {MA_DRV_NAME, 0};
struct spi_driver sdrv = {
        .driver = {
                .name = MA_DRV_NAME,
                .bus = &spi_bus_type,
                .owner = THIS_MODULE,
        },
        .probe = mas_probe,
        .remove = mas_remove,
        .id_table = &sdev_id,
};
//driver end
static struct mt_chip_conf smt_conf = {
    .setuptime=10,
    .holdtime=10,
    .high_time=10, // 10--6m 15--4m 20--3m 30--2m [ 60--1m 120--0.5m  300--0.2m]
    .low_time=10,
    .cs_idletime=10,
    .ulthgh_thrsh=0,
    .cpol=0,
    .cpha=0,
    .rx_mlsb=SPI_MSB,
    .tx_mlsb=SPI_MSB,
    .tx_endian=0,
    .rx_endian=0,
    .com_mod=FIFO_TRANSFER,
    .pause=0,
    .finish_intr=5,
    .deassert=0,
    .ulthigh=0,
    .tckdly=0,
};

struct spi_board_info smt_info[] __initdata = {
        [0] = {
                .modalias = MA_DRV_NAME,
                .max_speed_hz = (6*1000000),
                .bus_num = 0,
                .chip_select = 0,
                .mode = SPI_MODE_0,
                .controller_data = &smt_conf,
        },
};
//device end

/**
 *  the spi struct date start,for getting the spi_device to set the spi clock enable end
 */


void mas_select_transfer(struct spi_device *spi, int len) {
    static int mode = -1;
    int tmp = len>32? DMA_TRANSFER: FIFO_TRANSFER;
    struct mt_chip_conf *conf = NULL;
    if(tmp!=mode) {
        conf = (struct mt_chip_conf *) spi->controller_data;
        conf->com_mod = tmp;
        spi_setup(spi);
        mode = tmp;
    }
}

/*
 *  set spi speed, often we must check whether the setting is efficient
 */

void ma_spi_change(struct spi_device *spi, unsigned int  speed, int flag)
{
    struct mt_chip_conf *mcc = (struct mt_chip_conf *)spi->controller_data;
    if(flag == 0) { 
        mcc->com_mod = FIFO_TRANSFER;
    } else {
        mcc->com_mod = DMA_TRANSFER;
    }    
    switch(speed)
    {    
        case 500000:
            mcc->high_time = 120; 
            mcc->low_time = 120; 
            break;
        case 1000000:
            mcc->high_time = 60;
            mcc->low_time = 60;
            break;
        case 2000000:
            mcc->high_time = 30;
            mcc->low_time = 30;
            break;
        case 3000000:
            mcc->high_time = 20;
            mcc->low_time = 20;
            break;
        case 4000000:
            mcc->high_time = 15;
            mcc->low_time = 15;
            break;
        case 5000000:
            mcc->high_time = 10;
            mcc->low_time = 10;
            break;
        case 8000000:
            mcc->high_time = 8; 
            mcc->low_time = 8; 
            break;  
        default:
            MALOGD("default speed");
            break;
    }    
    if(spi_setup(spi) < 0){
        MALOGE("change the spi error!\n");
    }    
}





int mas_get_platform(void) {
	ret = platform_driver_register(&spdrv);
	if(ret){
		MALOGE("platform_driver_register");
	}
	ret = spi_register_board_info(smt_info, ARRAY_SIZE(smt_info));
	if(ret){
		MALOGE("spi_register_board_info");
	}
	ret = spi_register_driver(&sdrv);
	if(ret) {
		MALOGE("spi_register_driver");
	}
	return ret;
}

int mas_remove_platform(void){
	spi_unregister_driver(&sdrv);
	return 0;
}

int mas_finger_get_gpio_info(struct platform_device *pdev){
    struct device_node *node;
    //MALOGD("start!");
	node = of_find_compatible_node(NULL, NULL, MA_DTS_NAME);
	mas_finger_pinctrl = devm_pinctrl_get(&pdev->dev);
	if (IS_ERR(mas_finger_pinctrl)) {
		ret = PTR_ERR(mas_finger_pinctrl);
		dev_err(&pdev->dev, "mas_finger_pinctrl cannot find pinctrl\n");
		return ret;
	}

/**		this is the demo, setup follow the requirement
 *		mas_finger_eint_on = pinctrl_lookup_state(mas_finger_pinctrl, "finger_int_as_int");
 *		if (IS_ERR(mas_finger_eint_on)) {
 *			ret = PTR_ERR(mas_finger_eint_on);
 *			dev_err(&pdev->dev, " Cannot find mas_finger pinctrl mas_finger_eint_on!\n");
 *			return ret;
 *		}
 *      if needed, change the dts label and the pinctrl for the other gpio
 */
 	

 	/*mas_finger_power2v8_on = pinctrl_lookup_state(mas_finger_pinctrl, "finger_power_en1");
	if (IS_ERR(mas_finger_power2v8_on)) {
		ret = PTR_ERR(mas_finger_power2v8_on);
		dev_err(&pdev->dev, " Cannot find mas_finger_power2v8_on pinctrl!\n");
		return ret;
	}
 	mas_finger_power2v8_off = pinctrl_lookup_state(mas_finger_pinctrl, "finger_power_en0");
	if (IS_ERR(mas_finger_power2v8_off)) {
		ret = PTR_ERR(mas_finger_power2v8_off);
		dev_err(&pdev->dev, " Cannot find mas_finger_power2v8_off pinctrl!\n");
		return ret;
	}

	mas_finger_power1v8_on = pinctrl_lookup_state(mas_finger_pinctrl, "finger_power_18v_en1");
	if (IS_ERR(mas_finger_power1v8_on)) {
		ret = PTR_ERR(mas_finger_power1v8_on);
		dev_err(&pdev->dev, " Cannot find mas_finger_power1v8_on pinctrl!\n");
		return ret;
	}

	mas_finger_power1v8_off = pinctrl_lookup_state(mas_finger_pinctrl, "finger_power_18v_en0");
	if (IS_ERR(mas_finger_power1v8_off)) {
		ret = PTR_ERR(mas_finger_power1v8_off);
		dev_err(&pdev->dev, " Cannot find mas_finger_power1v8_off pinctrl!\n");
		return ret;
	}
*/
 	mas_spi_mi_on = pinctrl_lookup_state(mas_finger_pinctrl, "finger_miso");
	if (IS_ERR(mas_spi_mi_on)) {
		ret = PTR_ERR(mas_spi_mi_on);
		dev_err(&pdev->dev, " Cannot find mas_spi_mi_on pinctrl!\n");
		return ret;
	}
 	mas_spi_mi_off = pinctrl_lookup_state(mas_finger_pinctrl, "finger_miso");
	if (IS_ERR(mas_spi_mi_off)) {
		ret = PTR_ERR(mas_spi_mi_off);
		dev_err(&pdev->dev, " Cannot find mas_spi_mi_off pinctrl!\n");
		return ret;
	}
 	mas_spi_mo_on = pinctrl_lookup_state(mas_finger_pinctrl, "finger_mosi");
	if (IS_ERR(mas_spi_mo_on)) {
		ret = PTR_ERR(mas_spi_mo_on);
		dev_err(&pdev->dev, " Cannot find mas_spi_mo_on pinctrl!\n");
		return ret;
	}

	mas_spi_mo_off = pinctrl_lookup_state(mas_finger_pinctrl, "finger_mosi");
	if (IS_ERR(mas_spi_mo_off)) {
		ret = PTR_ERR(mas_spi_mo_off);
		dev_err(&pdev->dev, " Cannot find mas_spi_mo_off!\n");
		return ret;
	}

	mas_spi_ck_on = pinctrl_lookup_state(mas_finger_pinctrl, "finger_sck");
	if (IS_ERR(mas_spi_ck_on)) {
		ret = PTR_ERR(mas_spi_ck_on);
		dev_err(&pdev->dev, " Cannot find mas_spi_ck_on pinctrl!\n");
		return ret;
	}

	mas_spi_ck_off = pinctrl_lookup_state(mas_finger_pinctrl, "finger_sck");
	if (IS_ERR(mas_spi_ck_off)) {
		ret = PTR_ERR(mas_spi_ck_off);
		dev_err(&pdev->dev, " Cannot find mas_spi_ck_off pinctrl !\n");
		return ret;
	}

	mas_spi_cs_on = pinctrl_lookup_state(mas_finger_pinctrl, "finger_cs");
	if (IS_ERR(mas_spi_cs_on)) {
		ret = PTR_ERR(mas_spi_cs_on);
		dev_err(&pdev->dev, " Cannot find mas_spi_cs_on pinctrl!\n");
		return ret;
	}

	mas_spi_cs_off = pinctrl_lookup_state(mas_finger_pinctrl, "finger_cs");
	if (IS_ERR(mas_spi_cs_off)) {
		ret = PTR_ERR(mas_spi_cs_off);
		dev_err(&pdev->dev, " Cannot find mas_spi_cs_off pinctrl!\n");
		return ret;
	}

	mas_finger_eint_on = pinctrl_lookup_state(mas_finger_pinctrl, "finger_eint");
	if (IS_ERR(mas_finger_eint_on)) {
		ret = PTR_ERR(mas_finger_eint_on);
		dev_err(&pdev->dev, " Cannot find mas_finger_eint_on pinctrl!\n");
		return ret;
	}

        return 0;
}



int mas_finger_set_spi(int cmd){
//#if 0
	switch(cmd)
	{
		case 0:
			if( (!IS_ERR(mas_spi_cs_off)) & (!IS_ERR(mas_spi_ck_off)) & (!IS_ERR(mas_spi_mi_off)) & (!IS_ERR(mas_spi_mo_off)) ){
				pinctrl_select_state(mas_finger_pinctrl, mas_spi_cs_off);
				pinctrl_select_state(mas_finger_pinctrl, mas_spi_ck_off);
				pinctrl_select_state(mas_finger_pinctrl, mas_spi_mi_off);
				pinctrl_select_state(mas_finger_pinctrl, mas_spi_mo_off);
			}else{
				MALOGE("mas_spi_gpio_slect_pinctrl cmd=0 err!");
				return -1;
			}
			break;
		case 1:
			if( (!IS_ERR(mas_spi_cs_on)) & (!IS_ERR(mas_spi_ck_on)) & (!IS_ERR(mas_spi_mi_on)) & (!IS_ERR(mas_spi_mo_on)) ){
				pinctrl_select_state(mas_finger_pinctrl, mas_spi_cs_on);
				pinctrl_select_state(mas_finger_pinctrl, mas_spi_ck_on);
	        	pinctrl_select_state(mas_finger_pinctrl, mas_spi_mi_on);
				pinctrl_select_state(mas_finger_pinctrl, mas_spi_mo_on);
			}else{
				MALOGE("mas_spi_gpio_slect_pinctrl cmd=1 err!");
				return -1;
			}
			break;
	}
//#endif
	return 0;
}

int mas_finger_set_power(int cmd)
{
//#if 0
	switch (cmd)
		{
		case 0 : 		
			if( (!IS_ERR(mas_finger_power2v8_off)) & (!IS_ERR(mas_finger_power1v8_off)) ){
				pinctrl_select_state(mas_finger_pinctrl, mas_finger_power2v8_off);
				pinctrl_select_state(mas_finger_pinctrl, mas_finger_power1v8_off);
			}else{
				MALOGE("mas_power_gpio_slect_pinctrl cmd=0 err!");
				return -1;
			}
		break;
		case 1 : 		
			if( (!IS_ERR(mas_finger_power2v8_on)) & (!IS_ERR(mas_finger_power1v8_on)) ){
				pinctrl_select_state(mas_finger_pinctrl, mas_finger_power2v8_on);
				pinctrl_select_state(mas_finger_pinctrl, mas_finger_power1v8_on);
			}else{
				MALOGE("mas_power_gpio_slect_pinctrl cmd=1 err!");
				return -1;
			}
		break;
		}
//#endif
	return 0;
}

int mas_finger_set_eint(int cmd)
{
	switch (cmd)
		{
		case 0 : 		
			if(!IS_ERR(mas_finger_eint_off)){
				pinctrl_select_state(mas_finger_pinctrl, mas_finger_eint_off);
			}else{
				MALOGE("mas_eint_gpio_slect_pinctrl cmd=0 err!");
				return -1;
			}		
			break;
		case 1 : 		
			if(!IS_ERR(mas_finger_eint_on)){
				pinctrl_select_state(mas_finger_pinctrl, mas_finger_eint_on);
			}else{
				MALOGE("mas_eint_gpio_slect_pinctrl cmd=1 err!");
				return -1;
			}
			break;
		}
	return 0;
}


int mas_finger_set_gpio_info(int cmd){
	ret = 0;
	ret |= mas_finger_set_spi(cmd);
	//ret |= mas_finger_set_power(cmd);
	ret |= mas_finger_set_eint(cmd);
	return ret;
}

void mas_enable_spi_clock(struct spi_device *spi){
	mt_spi_enable_clk(spi_master_get_devdata(spi->master)); 
}

void mas_disable_spi_clock(struct spi_device *spi){
	mt_spi_disable_clk(spi_master_get_devdata(spi->master)); 
}

unsigned int mas_get_irq(void){
    struct device_node *node = NULL;
    //MALOGF("start");
    node = of_find_compatible_node(NULL, NULL, MA_EINT_DTS_NAME);
    return irq_of_parse_and_map(node, 0);
}
