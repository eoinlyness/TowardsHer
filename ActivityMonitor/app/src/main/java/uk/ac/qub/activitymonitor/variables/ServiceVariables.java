package uk.ac.qub.activitymonitor.variables;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


public class ServiceVariables {

    public List<SMSInfo> smsList = new ArrayList<SMSInfo>();
    public List<SMSInfo> smsOutList = new ArrayList<SMSInfo>();
    public List<CallInfo> callList = new ArrayList<CallInfo>();
    public List<LocationPoint> path = new ArrayList<LocationPoint>();
    public List<PlaceInfo> places = new ArrayList<PlaceInfo>();
    public List<ApplicationInformation> appInfo = new ArrayList<ApplicationInformation>();
    public List<URLInfo> urls = new ArrayList<URLInfo>();

    public ServiceVariables() {

    }
}
