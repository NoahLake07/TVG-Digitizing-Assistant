package com.thevideogoat.digitizingassistant.data;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.ListIterator;

public class Util {

    public static File renameFile(File file, String newName){
        String extension = "";
        int i = file.getName().lastIndexOf('.');
        if (i > 0) {
            extension = file.getName().substring(i);
        }
        File newFile = new File(file.getParentFile(), newName + extension);
        file.renameTo(newFile);
        return newFile;
    }

    public static boolean deleteFile(File file){
        return file.delete();
    }

    public static void renameLinkedFiles(Conversion c){
        if(c.linkedFiles.isEmpty()){
            return;
        }

        ListIterator<File> iterator = c.linkedFiles.listIterator();
        int i = 0;
        while(iterator.hasNext()){
            File f = iterator.next();
            ++i;
            // Rename the file and replace it in the list
            iterator.set(renameFile(f, c.name + (i > 1 ? " (" + i + ")" : "")));
        }

        // Completion dialog
        JOptionPane.showMessageDialog(null, "Renamed " + i + " files.", "Rename Success", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void renameFiles(ArrayList<File> files, String newName){
        if(files.isEmpty()){
            return;
        }

        int i = 0;
        for (File f : files){
            renameFile(f, newName + (i > 0 ? " (" + i + ")" : ""));
            i++;
        }
        JOptionPane.showMessageDialog(null, "Renamed " + i + " files.", "Rename Success", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void relinkFiles(Project project) {
        // Prompt the user to select a new directory
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = fileChooser.showOpenDialog(null);

        if (result == JFileChooser.APPROVE_OPTION) {
            File newDirectory = fileChooser.getSelectedFile();

            // For each conversion in the project
            int i = 0, f = 0;
            for (Conversion conversion : project.getConversions()) {
                ArrayList<File> updatedFiles = new ArrayList<>();

                // For each linked file in the conversion
                for (File oldFile : conversion.linkedFiles) {
                    File newFile = new File(newDirectory, oldFile.getName());

                    // If a file with the same name is found in the new directory
                    if (newFile.exists()) {
                        updatedFiles.add(newFile);
                        i++;
                    } else {
                        // If not found, keep the old file reference
                        updatedFiles.add(oldFile);
                        f++;
                    }
                }

                // Update the conversion's linked files
                conversion.linkedFiles = updatedFiles;
            }

            // Notify the user of completion
            JOptionPane.showMessageDialog(null, i+" files relinked successfully.", "Relink Success", JOptionPane.INFORMATION_MESSAGE);
            if(f>0) JOptionPane.showMessageDialog(null, f+" files couldn't be relinked.", "Relink Failure", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public static ArrayList<Conversion> sortConversionsBy(ArrayList<Conversion> conversions, String sortOption){
        if(sortOption.equals("Name")){
            for (int i = 0; i < conversions.size(); i++) {
                int x = 0; // index of item with the least value found

                // find the least value in range [i,size-1]
                for (int j = i; j < conversions.size(); j++) {
                    int comparison = conversions.get(j).name.compareTo(conversions.get(x).name);
                    if(comparison < 0){
                        x = j;
                    }
                }

                // swap the least value with the value at i
                Conversion temp = conversions.get(i);
                conversions.set(i,conversions.get(x));
                conversions.set(x,temp);
            }
        }

        return conversions;
    }
}