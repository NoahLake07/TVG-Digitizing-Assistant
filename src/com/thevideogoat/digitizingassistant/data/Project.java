package com.thevideogoat.digitizingassistant.data;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Project implements Serializable {

    String name;
    ArrayList<Conversion> conversions;

    public Project(String name){
        conversions = new ArrayList<>();
        this.name = name;
    }

    public Project (Path source){
        // deserialize the project from the source
        try {
            FileInputStream fileIn = new FileInputStream(source.toString());
            ObjectInputStream objectInputStream = new ObjectInputStream(fileIn);
            Project project = (Project) objectInputStream.readObject();
            this.name = project.name;
            this.conversions = project.conversions;
            objectInputStream.close();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void addConversion(Conversion conversion){
        conversions.add(conversion);
    }

    public void saveToFile(Path destination) {
        File projectFile = Paths.get(destination.toString(), name + ".project").toFile();

        try {
            System.out.println("> Saving project to file: " + projectFile.toPath());

            // Serialize this class to the destination
            FileOutputStream fileOut = new FileOutputStream(projectFile);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOut);
            objectOutputStream.writeObject(this);

        } catch (IOException e) {
            throw new Error("Failed to save project to file.", e);
        }
    }

    public String getName(){
        return this.name;
    }

    public ArrayList<Conversion> getConversions(){
        return this.conversions;
    }

}
