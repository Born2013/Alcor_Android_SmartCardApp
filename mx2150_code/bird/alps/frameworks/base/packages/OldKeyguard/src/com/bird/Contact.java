package com.android.keyguard;

/**
 * Created by root on 17-8-18.
 */

public class Contact {


    private String PhoneNumber;

    private String Name;

    private boolean isLauncherContact;

    private byte[] imageByte;


    private boolean isCall;

    public Contact(String phoneNumber, String name, boolean isLauncherContact, byte[] imageByte) {
        PhoneNumber = phoneNumber;
        Name = name;
        this.isLauncherContact = isLauncherContact;
        this.imageByte = imageByte;
    }


    public Contact(String phoneNumber, String name, byte[] imageByte) {
        PhoneNumber = phoneNumber;
        Name = name;
        this.imageByte = imageByte;

    }


    public String getPhoneNumber() {
        return PhoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        PhoneNumber = phoneNumber;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public boolean isLauncherContact() {
        return isLauncherContact;
    }

    public void setLauncherContact(boolean launcherContact) {
        isLauncherContact = launcherContact;
    }

    public byte[] getImageByte() {
        return imageByte;
    }

    public void setImageByte(byte[] imageByte) {
        this.imageByte = imageByte;
    }

    public boolean isCall() {
        return isCall;
    }

    public void setCall(boolean call) {
        isCall = call;
    }


    public static boolean isTheSameContact(Contact obj1, Contact obj2) {
        return obj1.getPhoneNumber().equals(obj2.getPhoneNumber());
    }


    @Override
    public String toString() {
        return "PhoneNumber = " + PhoneNumber
                + ", Name = " + Name;
    }
}
