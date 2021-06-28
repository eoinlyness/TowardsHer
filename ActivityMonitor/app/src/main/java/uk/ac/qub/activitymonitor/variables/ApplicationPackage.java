package uk.ac.qub.activitymonitor.variables;

import java.util.ArrayList;


public class ApplicationPackage implements Comparable<ApplicationPackage> {

    public String packageName = "";
    public String applicationName = "";

    public ApplicationPackage(String packageName, String applicationName) {
        this.packageName = packageName;
        this.applicationName = applicationName;
    }


    /**
     * Return String ArrayList (Application name) from ApplicationPackage ArrayList
     *
     * @param Applications ApplicationPackage ArrayList
     * @return ApplicationPackage ArrayList - Application Name
     */
    public static ArrayList<String> getApplicationNames(ArrayList<ApplicationPackage> Applications) {
        ArrayList<String> name = new ArrayList<String>();

        for (ApplicationPackage app : Applications) {

            name.add(app.applicationName);
        }

        return name;
    }


    /**
     * Return String ArrayList (Package name) from ApplicationPackage ArrayList
     *
     * @param Applications ApplicationPackage ArrayList
     * @return ApplicationPackage ArrayList - Package name
     */
    public static ArrayList<String> getPackageNames(ArrayList<ApplicationPackage> Applications) {
        ArrayList<String> name = new ArrayList<String>();

        for (ApplicationPackage app : Applications) {

            name.add(app.packageName);
        }

        return name;
    }


    @Override
    public int compareTo(ApplicationPackage another) {
        if (another.packageName.equals(packageName))
            return 1;
        else
            return 0;
    }


    @Override
    public boolean equals(Object object) {
        if (object.getClass() == ApplicationPackage.class) {
            if (((ApplicationPackage) object).packageName.equals(packageName))
                return true;
            else
                return false;
        } else
            return false;
    }

}
