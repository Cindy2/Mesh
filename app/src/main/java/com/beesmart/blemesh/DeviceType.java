package com.beesmart.blemesh;

public enum DeviceType {

	LED(1, R.drawable.ic_type_led),

	Buzzer(2, R.drawable.ic_type_buzzer),

	RGB(3, R.drawable.ic_type_rgb),

	CCTW(4, R.drawable.ic_type_light),

	Switch(10, R.drawable.ic_type_switch),
	
	Unknown(255, R.drawable.ic_type_bluetooth), ;

	int type;
	int drawble;

	private DeviceType(int type, int drawble) {
		this.type = type;
		this.drawble = drawble;
	}

	public int getTypeInt() {
		return type;
	}

	public int getDrawable() {
		return drawble;
	}

	public static DeviceType getType(int type) {
		for (DeviceType taxType : DeviceType.values()) {
			if (taxType.type == type) {
				return taxType;
			}
		}
		return Unknown;
	}

}
