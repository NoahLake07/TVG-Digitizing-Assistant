package com.thevideogoat.digitizingassistant.debug;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UMLDiagramGenerator {

    // List to hold the parsed Java classes
    private List<JavaClass> classes = new ArrayList<>();

    public static void main(String[] args) {

        // open file chooser for project directory
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("Select Project Directory");
        fileChooser.setApproveButtonText("Select");
        fileChooser.showOpenDialog(null);

        if(fileChooser.getSelectedFile() == null) {
            System.out.println("No directory selected.");
            return;
        }

        String projectDir = fileChooser.getSelectedFile().getAbsolutePath();
        UMLDiagramGenerator generator = new UMLDiagramGenerator();
        generator.scanDirectory(new File(projectDir));
        generator.generateUML();
    }

    // Recursively scan a directory for .java files
    public void scanDirectory(File dir) {
        if (dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                if (file.isDirectory()) {
                    scanDirectory(file);
                } else if (file.getName().endsWith(".java")) {
                    parseJavaFile(file);
                }
            }
        }
    }

    // Parse a Java file to extract class name, fields, and methods.
    public void parseJavaFile(File file) {
        try {
            List<String> lines = Files.readAllLines(file.toPath());
            JavaClass currentClass = null;

            for (String line : lines) {
                line = line.trim();

                // Skip comments or empty lines
                if (line.startsWith("//") || line.startsWith("*") || line.startsWith("/*") || line.isEmpty()) {
                    continue;
                }

                // Look for a class declaration (simple regex; might need improvement for generics or interfaces)
                Pattern classPattern = Pattern.compile(".*\\bclass\\s+(\\w+).*");
                Matcher classMatcher = classPattern.matcher(line);
                if (classMatcher.find()) {
                    String className = classMatcher.group(1);
                    currentClass = new JavaClass(className);
                    classes.add(currentClass);
                    continue;
                }

                // If we are inside a class, try to find fields and methods
                if (currentClass != null) {
                    // Field pattern: visibility, type, name ending with semicolon
                    Pattern fieldPattern = Pattern.compile("(public|private|protected)\\s+([\\w<>\\[\\]]+)\\s+(\\w+)\\s*;");
                    Matcher fieldMatcher = fieldPattern.matcher(line);
                    if (fieldMatcher.find()) {
                        String visibility = fieldMatcher.group(1);
                        String type = fieldMatcher.group(2);
                        String name = fieldMatcher.group(3);
                        currentClass.fields.add(visibility + " " + name + " : " + type);
                        continue;
                    }

                    // Method pattern: visibility, return type, method name with parameters
                    Pattern methodPattern = Pattern.compile("(public|private|protected)\\s+([\\w<>\\[\\]]+)\\s+(\\w+)\\s*\\([^)]*\\)\\s*\\{?");
                    Matcher methodMatcher = methodPattern.matcher(line);
                    if (methodMatcher.find()) {
                        String visibility = methodMatcher.group(1);
                        String returnType = methodMatcher.group(2);
                        String methodName = methodMatcher.group(3);
                        currentClass.methods.add(visibility + " " + methodName + "() : " + returnType);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + file.getAbsolutePath());
            e.printStackTrace();
        }
    }

    // Generate a PlantUML diagram text that you can render with PlantUML tools
    public void generateUML() {
        System.out.println("@startuml");
        for (JavaClass jc : classes) {
            System.out.println("class " + jc.name + " {");
            for (String field : jc.fields) {
                System.out.println("  " + field);
            }
            for (String method : jc.methods) {
                System.out.println("  " + method);
            }
            System.out.println("}");
        }
        System.out.println("@enduml");
    }

    // Inner class to represent a parsed Java class
    class JavaClass {
        String name;
        List<String> fields;
        List<String> methods;

        JavaClass(String name) {
            this.name = name;
            this.fields = new ArrayList<>();
            this.methods = new ArrayList<>();
        }
    }
}
