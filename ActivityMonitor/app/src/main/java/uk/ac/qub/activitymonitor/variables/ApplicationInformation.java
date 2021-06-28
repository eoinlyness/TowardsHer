package uk.ac.qub.activitymonitor.variables;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;


public class ApplicationInformation extends ApplicationPackage {

    public String time = "";

    public ApplicationInformation(String packageName, String applicationName) {
        super(packageName, applicationName);
        this.time = getCurrentTime();
    }

    public ApplicationInformation(String packageName, String applicationName, String time) {
        super(packageName, applicationName);
        this.time = time;
    }

    /**
     * Get current time in HH:mm:ss format
     *
     * @return String time in HH:mm:ss format
     */
    public static String getCurrentTime() {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
        return format.format(Calendar.getInstance().getTime());
    }

    /**
     * Check if last addition to arraylist equals packagename
     *
     * @param applications ApplicationInformation ArrayList
     * @param packageName  PackageName to check
     * @return Boolean - true if package name is at end of arraylist
     */
    public static boolean isLastApp(List<ApplicationInformation> applications, String packageName) {

        if (applications.size() == 0)
            return false;

        ApplicationInformation app = applications.get(applications.size()-1);
        return packageName.equals(app.packageName);
    }

    /**
     * Get position in arraylist containing specific package name
     *
     * @param applications ApplicationInformation ArrayList
     * @param packageName  PackageName to check
     * @return int position
     */
    public static int getPosition(List<ApplicationInformation> applications, String packageName) {
        for (int i = 0; i < applications.size(); i++) {
            if (packageName.equals(applications.get(i).packageName))
                return i;
        }

        return -1;
    }


}
