package fxsampler.util;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import fxsampler.FXSamplerProject;
import fxsampler.Sample;
import fxsampler.model.EmptySample;
import fxsampler.model.Project;

/**
 * All the code related to classpath scanning, etc for samples.
 */
public class SampleScanner {

    private static List<String> ILLEGAL_CLASS_NAMES = new ArrayList<>();

    static {
        ILLEGAL_CLASS_NAMES.add( "/com/javafx/main/Main.class" );
        ILLEGAL_CLASS_NAMES.add( "/com/javafx/main/NoJavaFXFallback.class" );
    }

    private static Map<String, FXSamplerProject> packageToProjectMap = new HashMap<>();

    static {
        System.out.println( "Initialising FXSampler sample scanner..." );
        System.out.println( "\tDiscovering projects..." );
        // find all projects on the classpath that expose a FXSamplerProject
        // service. These guys are our friends....
        ServiceLoader<FXSamplerProject> loader = ServiceLoader.load( FXSamplerProject.class );
        for ( FXSamplerProject project : loader ) {
            final String projectName = project.getProjectName();
            final String basePackage = project.getSampleBasePackage();
            packageToProjectMap.put( basePackage, project );
            System.out.println( "\t\tFound project '" + projectName
                    + "', with sample base package '" + basePackage + "'" );
        }

        if ( packageToProjectMap.isEmpty() ) {
            System.out.println( "\tError: Did not find any projects!" );
        }
    }

    private final Map<String, Project> projectsMap = new HashMap<>();

    /**
     * Gets the list of sample classes to load
     *
     * @return The classes
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public Map<String, Project> discoverSamples() {
        String[] classes = {
            "org.controlsfx.samples.actions.HelloActionGroup",
            "org.controlsfx.samples.actions.HelloActionProxy",
            "org.controlsfx.samples.textfields.HelloAutoComplete",
            "org.controlsfx.samples.HelloBorders",
            "org.controlsfx.samples.button.HelloBreadCrumbBar",
            "org.controlsfx.samples.button.HelloButtonBar",
            "org.controlsfx.samples.checked.HelloCheckComboBox",
            "org.controlsfx.samples.checked.HelloCheckListView",
            "org.controlsfx.samples.checked.HelloCheckTreeView",
            "org.controlsfx.samples.HelloDecorator",
            "org.controlsfx.samples.dialogs.HelloDialogs",
            "org.controlsfx.samples.HelloGlyphFont",
            "org.controlsfx.samples.HelloGridView",
            "org.controlsfx.samples.HelloHiddenSidesPane",
            "org.controlsfx.samples.HelloHyperlinkLabel",
            "org.controlsfx.samples.HelloInfoOverlay",
            "org.controlsfx.samples.HelloListSelectionView",
            "org.controlsfx.samples.HelloMasterDetailPane",
            "org.controlsfx.samples.HelloNotificationPane",
            "org.controlsfx.samples.HelloNotifications",
            "org.controlsfx.samples.HelloPlusMinusSlider",
            "org.controlsfx.samples.HelloPopOver",
            "org.controlsfx.samples.HelloPropertySheet",
            "org.controlsfx.samples.HelloRangeSlider",
            "org.controlsfx.samples.HelloRating",
            "org.controlsfx.samples.button.HelloSegmentedButton",
            "org.controlsfx.samples.HelloSnapshotView",
            "org.controlsfx.samples.HelloSpreadsheetView",
            "org.controlsfx.samples.HelloStatusBar",
            "org.controlsfx.samples.HelloTaskProgressView",
            "org.controlsfx.samples.textfields.HelloTextFields",
            "org.controlsfx.samples.HelloValidation"
        };

        List<Class<?>> results = new ArrayList<>();
        
        for(String className: classes){
            try {
                results.add( Class.forName( className));
            }
            catch ( ClassNotFoundException ex ) {
                // Skip
            }
        }

        for ( Class<?> sampleClass : results ) {
            if ( !Sample.class.isAssignableFrom( sampleClass ) ) {
                continue;
            }
            if ( sampleClass.isInterface() ) {
                continue;
            }
            if ( Modifier.isAbstract( sampleClass.getModifiers() ) ) {
                continue;
            }
//            if (Sample.class.isAssignableFrom(EmptySample.class)) continue;
            if ( sampleClass == EmptySample.class ) {
                continue;
            }

            Sample sample = null;
            try {
                sample = (Sample)sampleClass.newInstance();
            }
            catch ( InstantiationException | IllegalAccessException e ) {
                e.printStackTrace();
            }
            if ( sample == null || !sample.isVisible() ) {
                continue;
            }

            final String packageName = sampleClass.getPackage().getName();

            for ( String key : packageToProjectMap.keySet() ) {
                if ( packageName.contains( key ) ) {
                    final String prettyProjectName = packageToProjectMap.get( key ).getProjectName();

                    Project project;
                    if ( !projectsMap.containsKey( prettyProjectName ) ) {
                        project = new Project( prettyProjectName, key );
                        project.setWelcomePage( packageToProjectMap.get( key ).getWelcomePage() );
                        projectsMap.put( prettyProjectName, project );
                    }
                    else {
                        project = projectsMap.get( prettyProjectName );
                    }

                    project.addSample( packageName, sample );
                }
            }
        }

        return projectsMap;
    }

    
}
