package com.thevideogoat.digitizingassistant.data;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;

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
        ArrayList<File> files = c.linkedFiles;

        if(files.isEmpty()){
            return;
        }

        int i = 0;
        for (File f : files){
            c.linkedFiles.remove(f);
            c.linkedFiles.add(renameFile(f, c.name + (i > 0 ? " (" + i + ")" : "")));
            i++;
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
}