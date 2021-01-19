package com.process;

import java.io.*;
import java.util.*;

public class Main
{
    public static final String CSV = "CSV";
    public static final List<String> alErrors = new ArrayList<>();
    public static String sOutputPath = "C:/Users/nicol/Desktop";
    public static String sInputFile = "C:/Users/nicol/Desktop/enrollment_test.csv";

    public static void main(String[] args)
    {
        if(args.length != 2)
        {
            System.out.println("The following two arguements are required: (1) Input File Name and Path (2) Path for output file(s)");
        }
        else
        {
            sInputFile = args[0];
            sOutputPath = args[1];

            try
            {
                File fImportFile = new File(sInputFile);

                // Verify that the file is correct type - extension is csv
                if (!CSV.equalsIgnoreCase(getFileExtension(fImportFile.getName()))) {
                    System.out.println("ERROR: File type must be CSV.");
                } else {
                    Map<String, List<User>> companyListMap = new HashMap<>();

                    processFile(fImportFile, companyListMap);

                    separateFileByCompany(companyListMap);
                }

                for (String message : alErrors)
                {
                    System.out.println(message);
                }
            } catch (FileNotFoundException e)
            {
                System.out.println("An error has occurred. " + e);
            }
        }
    }


    /**
     * Process the file.
     * @param fImportFile The File to import.
     * @param companyListMap Map containing a list of users by company.
     * @throws FileNotFoundException throw exception if one occurs while reading the file.
     */
    private static void processFile(final File fImportFile, final Map<String, List<User>> companyListMap) throws FileNotFoundException
    {
        int lineCount = 0;
        Scanner reader = new Scanner(fImportFile);

        List<String> recErrors = new ArrayList<>();

        while (reader.hasNextLine())
        {
            lineCount++;

            String data = reader.nextLine();
            List<String> recordList = Arrays.asList(data.split(","));

            if(recordList.size() == 4)
            {
                final User user = new User();

                // Validate and set User Id on object
                if(recordList.get(0) != null)
                {
                    user.setUserId(recordList.get(0));
                }
                else
                {
                    recErrors.add("Line " + lineCount + ": Error - User Id is required.");
                }

                // Validate and set Name on object
                if(recordList.get(1) != null)
                {
                    final String[] aName = recordList.get(1).split(" ");
                    user.setFirstName(aName[0]);
                    user.setLastName(aName[1]);
                }
                else
                {
                    recErrors.add("Line " + lineCount + ": Error - First and Last Name are required.");
                }

                // Validate and set Version on object
                if(recordList.get(2) != null && checkNumeric(recordList.get(2)))
                {
                    user.setVersion(new Integer(recordList.get(2)));
                }
                else
                {
                    recErrors.add("Line " + lineCount + ": Error - Version is required and must be numeric.");
                }

                // Validate and set Insurance Company on object
                if(recordList.get(3) != null)
                {
                    user.setCompany(recordList.get(3));
                }
                else
                {
                    recErrors.add("Line " + lineCount + ": Error - Insurance Company is required.");
                }

                // Check if company has been added to map yet; if not then add it along with the User
                // Otherwise check to see if the User record needs to be updated or added to the company
                if(!companyListMap.containsKey(user.getCompany()) && recErrors.isEmpty())
                {
                    List<User> companyUsers = new ArrayList<>();
                    companyUsers.add(user);

                    companyListMap.put(user.getCompany(), companyUsers);
                }
                // Check if user exists for company
                else
                {
                    // Retrieve the list of users for the company
                    List<User> companyUsers = companyListMap.get(user.getCompany());

                    // Check to see if user id already exists for Company
                    if(companyUsers.stream().anyMatch(x ->x.getUserId().equals(user.getUserId())))
                    {
                        final User userMatch = companyUsers.stream().filter(x -> x.getUserId().equals(user.getUserId())).findFirst().get();

                        // Need to replace user with the latest version if an older version is found for the company
                        if(userMatch.getVersion() < user.getVersion())
                        {
                            companyUsers.remove(userMatch);
                            companyUsers.add(user);
                        }
                    }
                    else
                    {
                        companyUsers.add(user);
                        companyListMap.put(user.getCompany(), companyUsers);
                    }
                }
            }
            // Invalid format - Four fields are expected: User Id, Name, Version, Insurance Company
            else
            {
                recErrors.add("Line " + lineCount + ": Error - Invalid Format. Four items expected but " + recordList.size()+ " found.");
            }

            // Add current record errors to list of all records.
            alErrors.addAll(recErrors);
            recErrors.clear();
        }
    }


    /**
     * Create new files separated by company.
     * @param companyListMap Map containing a list of users by company.
     * @throws FileNotFoundException throw exception if one occurs while creating the file.
     */
    private static void separateFileByCompany(final Map<String, List<User>> companyListMap) throws FileNotFoundException
    {
        for(Map.Entry<String,List<User>> entry : companyListMap.entrySet())
        {
            final String sFileName = entry.getKey() + ".csv";

            List<User> userList = entry.getValue();

            // Sort users for each company by Last Name and First Name
            userList.sort(Comparator.comparing((User u) -> (u.getLastName() + u.getFirstName())));

            StringBuilder sbContent = new StringBuilder();

            int iLineCount = 0;

            for(User user: userList)
            {
                iLineCount++;
                sbContent.append(user.getUserId()).append(",").append(user.getFirstName()).append(" ").append(user.getLastName())
                        .append(",").append(user.getVersion()).append(",").append(user.getCompany());

                if(iLineCount < userList.size())
                {
                    sbContent.append("\n");
                }
            }

            writeFile(sFileName, sbContent.toString());
        }
    }

    /**
     * Get the file extension.
     * @param fileName the {@link String} name of the file.
     * @return the {@link String} for the file extension.
     */
    private static String getFileExtension(final String fileName)
    {
        String fileExtension = "";
        if(fileName != null && fileName.lastIndexOf(".") > -1)
        {
            fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1);
        }
        return fileExtension;
    }

    /**
     * Determine if a field value is numeric.
     * @param field the {@link String} value of the field.
     * @return true if field value is numeric; false otherwise
     */
    private static boolean checkNumeric(final String field)
    {
        boolean bNumeric = true;

        if(field == null)
        {
            bNumeric = false;
        }
        else
        {
            int size = field.length();

            for(int i=0; i<size; i++)
            {
                if(!Character.isDigit(field.charAt(i)))
                {
                    bNumeric = false;
                }
            }
        }

        return  bNumeric;
    }

    /**
     * Write a new file to the specified path.
     * @param sFileName the file name.
     * @param sContent the content for the file.
     * @throws FileNotFoundException throw exception is one occurs while creating the file.
     */
    private static void writeFile(final String sFileName, final String sContent) throws FileNotFoundException
    {
        FileOutputStream fOut = new FileOutputStream(sOutputPath + "/" + sFileName);
        BufferedOutputStream bOut = new BufferedOutputStream(fOut);
        PrintWriter pOut = new PrintWriter(bOut);
        pOut.write(sContent);
        pOut.flush();
        pOut.close();
    }
}
