package com.example.absensireact.exel;

import com.example.absensireact.model.Absensi;
import com.example.absensireact.model.User;
import com.example.absensireact.repository.AbsensiRepository;
import com.example.absensireact.repository.UserRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Service
public class ExcelAbsnesiBulanan {
    @Autowired
    private AbsensiRepository absensiRepository;

    @Autowired
    private UserRepository userRepository;

    private String getMonthName(int month) {
        String[] monthNames = new DateFormatSymbols().getMonths();
        int index = month - 1;
        if (index >= 0 && index < monthNames.length) {
            return monthNames[index];
        }
        return "Bulan Tidak Valid";
    }


    public void excelAbsensiBulanan(int month, int year, HttpServletResponse response) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Absensi-Bulanan");

        // Define font color for header
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());

        // Cell style for header with light blue background and white font
        CellStyle styleHeader = workbook.createCellStyle();
        styleHeader.setAlignment(HorizontalAlignment.CENTER);
        styleHeader.setVerticalAlignment(VerticalAlignment.CENTER);
        styleHeader.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        styleHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styleHeader.setBorderTop(BorderStyle.THIN);
        styleHeader.setBorderRight(BorderStyle.THIN);
        styleHeader.setBorderBottom(BorderStyle.THIN);
        styleHeader.setBorderLeft(BorderStyle.THIN);
        styleHeader.setFont(headerFont);

        // Style for title (remains unchanged)
        CellStyle styleTitle = workbook.createCellStyle();
        styleTitle.setAlignment(HorizontalAlignment.CENTER);
        styleTitle.setVerticalAlignment(VerticalAlignment.CENTER);
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        styleTitle.setFont(titleFont);

        // Style for center-aligned cells with borders
        CellStyle styleCenterNumber = workbook.createCellStyle();
        styleCenterNumber.setAlignment(HorizontalAlignment.CENTER);
        styleCenterNumber.setVerticalAlignment(VerticalAlignment.CENTER);
        styleCenterNumber.setBorderTop(BorderStyle.THIN);
        styleCenterNumber.setBorderRight(BorderStyle.THIN);
        styleCenterNumber.setBorderBottom(BorderStyle.THIN);
        styleCenterNumber.setBorderLeft(BorderStyle.THIN);

        // Style for weekends with light blue background and borders
        CellStyle styleWeekend = workbook.createCellStyle();
        styleWeekend.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        styleWeekend.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styleWeekend.setBorderTop(BorderStyle.THIN);
        styleWeekend.setBorderRight(BorderStyle.THIN);
        styleWeekend.setBorderBottom(BorderStyle.THIN);
        styleWeekend.setBorderLeft(BorderStyle.THIN);

        // Style for cells containing "X" with bright red background
        CellStyle styleRedX = workbook.createCellStyle();
        styleRedX.setFillForegroundColor(IndexedColors.RED.getIndex());
        styleRedX.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styleRedX.setAlignment(HorizontalAlignment.CENTER);
        styleRedX.setVerticalAlignment(VerticalAlignment.CENTER);
        styleRedX.setBorderTop(BorderStyle.THIN);
        styleRedX.setBorderRight(BorderStyle.THIN);
        styleRedX.setBorderBottom(BorderStyle.THIN);
        styleRedX.setBorderLeft(BorderStyle.THIN);

        // Fetch data
        List<Absensi> absensiList = absensiRepository.findByMonthAndYear(month, year);

        // Determine the number of days in the month
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, 1);
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        if (absensiList.isEmpty()) {
            // Handle case when there are no absences for the given month and year
            Row emptyRow = sheet.createRow(0);
            Cell emptyCell = emptyRow.createCell(0);
            emptyCell.setCellValue("Tidak ada data absensi untuk bulan " + getMonthName(month) + " tahun " + year);
            emptyCell.setCellStyle(styleCenterNumber);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, daysInMonth + 1)); // Merge cells for message
        } else {
            // Group by user
            Map<String, List<Absensi>> userAbsensiMap = new HashMap<>();
            for (Absensi absensi : absensiList) {
                userAbsensiMap.computeIfAbsent(absensi.getUser().getUsername(), k -> new ArrayList<>()).add(absensi);
            }

            int rowNum = 0;

            // Title row
            Row titleRow = sheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("DATA ABSENSI BULAN : " + getMonthName(month) + " - " + year);
            titleCell.setCellStyle(styleTitle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, daysInMonth + 1)); // Merging cells for title
            rowNum++;

            // Header row
            Row headerRow = sheet.createRow(rowNum++);
            Cell cell = headerRow.createCell(0);
            cell.setCellValue("No");
            cell.setCellStyle(styleHeader);

            Cell nameHeader = headerRow.createCell(1);
            nameHeader.setCellValue("Name");
            nameHeader.setCellStyle(styleHeader);

            // Adding the date columns
            for (int i = 1; i <= daysInMonth; i++) {
                Cell dateHeader = headerRow.createCell(1 + i);
                dateHeader.setCellValue(i + "");
                dateHeader.setCellStyle(styleHeader);
            }

            // Data rows
            int userRowNum = 1;
            for (Map.Entry<String, List<Absensi>> entry : userAbsensiMap.entrySet()) {
                String userName = entry.getKey();
                List<Absensi> userAbsensi = entry.getValue();

                // User row
                Row userRow = sheet.createRow(rowNum++);
                Cell noCell = userRow.createCell(0);
                noCell.setCellValue(userRowNum++);
                noCell.setCellStyle(styleCenterNumber); // Border for No column

                Cell nameCell = userRow.createCell(1);
                nameCell.setCellValue(userName);
                nameCell.setCellStyle(styleCenterNumber); // Border for Name column

                // Initialize date columns
                Calendar dateCal = Calendar.getInstance();
                dateCal.set(year, month - 1, 1);

                for (int i = 1; i <= daysInMonth; i++) {
                    dateCal.set(year, month - 1, i);
                    Date currentDate = dateCal.getTime();
                    int dayOfWeek = dateCal.get(Calendar.DAY_OF_WEEK);
                    Cell cellForDate = userRow.createCell(1 + i);

                    if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                        // For Saturday and Sunday
                        cellForDate.setCellValue("-");
                        cellForDate.setCellStyle(styleWeekend); // Blue background for weekends
                    } else {
                        // For weekdays
                        Optional<Absensi> absensiForDate = userAbsensi.stream()
                                .filter(absensi -> isSameDay(absensi.getTanggalAbsen(), currentDate))
                                .findFirst();

                        if (absensiForDate.isPresent()) {
                            Absensi absensi = absensiForDate.get();
                            if ("Izin".equalsIgnoreCase(absensi.getStatusAbsen())) {
                                cellForDate.setCellValue("i"); // Mark "Izin" with "i"
                                cellForDate.setCellStyle(styleCenterNumber); // Default style
                            } else {
                                cellForDate.setCellValue("✓"); // Mark present with a checkmark
                                cellForDate.setCellStyle(styleCenterNumber); // Default style
                            }
                        } else {
                            cellForDate.setCellValue("X"); // Mark no data with "X"
                            cellForDate.setCellStyle(styleRedX); // Red background for "X"
                        }
                    }
                    cellForDate.setCellStyle(styleCenterNumber); // Center alignment and border
                }
            }

            // Set specific column widths
            sheet.setColumnWidth(0, 5 * 256); // Column No: 10 width
            sheet.setColumnWidth(1, 25 * 256); // Column Name: 50 width
            for (int i = 2; i <= daysInMonth + 1; i++) {
                sheet.setColumnWidth(i, 5 * 256); // Column Date: 20 width
            }

            // Adding the notes at the bottom of the table
            Row noteRow1 = sheet.createRow(rowNum++);
            Cell noteCell1 = noteRow1.createCell(0);
            noteCell1.setCellValue("X = tidak absen");

            Row noteRow2 = sheet.createRow(rowNum++);
            Cell noteCell2 = noteRow2.createCell(0);
            noteCell2.setCellValue("✓ = absen");

            Row noteRow3 = sheet.createRow(rowNum++);
            Cell noteCell3 = noteRow3.createCell(0);
            noteCell3.setCellValue("- = hari libur");

            Row noteRow4 = sheet.createRow(rowNum++);
            Cell noteCell4 = noteRow4.createCell(0);
            noteCell4.setCellValue("i = izin");

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=AbsensiBulanan_" + getMonthName(month) + "_" + year + ".xlsx");
            workbook.write(response.getOutputStream());
            workbook.close();
        }
    }

    private boolean isSameDay(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);

        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
    }








    public void excelAbsensiBulananSimpel(int month, int year, HttpServletResponse response) throws IOException, ParseException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Absensi-Simpel");

        // Cell styles
        CellStyle styleHeader = workbook.createCellStyle();
        styleHeader.setAlignment(HorizontalAlignment.CENTER);
        styleHeader.setVerticalAlignment(VerticalAlignment.CENTER);
        styleHeader.setBorderTop(BorderStyle.THIN);
        styleHeader.setBorderRight(BorderStyle.THIN);
        styleHeader.setBorderBottom(BorderStyle.THIN);
        styleHeader.setBorderLeft(BorderStyle.THIN);

        CellStyle styleTitle = workbook.createCellStyle();
        styleTitle.setAlignment(HorizontalAlignment.CENTER);
        styleTitle.setVerticalAlignment(VerticalAlignment.CENTER);
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        styleTitle.setFont(titleFont);

        CellStyle styleNumber = workbook.createCellStyle();
        styleNumber.setAlignment(HorizontalAlignment.CENTER);
        styleNumber.setVerticalAlignment(VerticalAlignment.CENTER);
        styleNumber.setBorderTop(BorderStyle.THIN);
        styleNumber.setBorderRight(BorderStyle.THIN);
        styleNumber.setBorderBottom(BorderStyle.THIN);
        styleNumber.setBorderLeft(BorderStyle.THIN);

        // Conditional formatting colors
        CellStyle styleColorLate = workbook.createCellStyle();
        styleColorLate.cloneStyleFrom(styleNumber);
        styleColorLate.setFillForegroundColor(IndexedColors.RED.getIndex());
        styleColorLate.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle styleColorPermission = workbook.createCellStyle();
        styleColorPermission.cloneStyleFrom(styleNumber);
        styleColorPermission.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
        styleColorPermission.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle styleColorEarly = workbook.createCellStyle();
        styleColorEarly.cloneStyleFrom(styleNumber);
        styleColorEarly.setFillForegroundColor(IndexedColors.GREEN.getIndex());
        styleColorEarly.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle styleColorEmpty = workbook.createCellStyle();
        styleColorEmpty.cloneStyleFrom(styleNumber);
        styleColorEmpty.setFillForegroundColor(IndexedColors.BROWN.getIndex());
        styleColorEmpty.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle styleColorLateColumn = workbook.createCellStyle();
        styleColorLateColumn.cloneStyleFrom(styleNumber);
        styleColorLateColumn.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
        styleColorLateColumn.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle styleColorPulangColumn = workbook.createCellStyle();
        styleColorPulangColumn.cloneStyleFrom(styleNumber);
        styleColorPulangColumn.setFillForegroundColor(IndexedColors.BROWN.getIndex());
        styleColorPulangColumn.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Fetch data
        List<Absensi> absensiList = absensiRepository.findByMonthAndYear(month, year);

        if (absensiList.isEmpty()) {
            // Handle case when there are no absences for the given month and year
            Row emptyRow = sheet.createRow(0);
            Cell emptyCell = emptyRow.createCell(0);
            emptyCell.setCellValue("Tidak ada data absensi simpel untuk bulan " + getMonthName(month) + "-" + year);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6)); // Merge cells for message
        } else {
            int rowNum = 0;

            // Title row
            Row titleRow = sheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("DATA ABSENSI SIMPEL : " + getMonthName(month));
            titleCell.setCellStyle(styleTitle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 6)); // Merging cells for title
            rowNum++;

            // Header row
            Row headerRow = sheet.createRow(rowNum++);
            String[] headers = {"No", "Nama", "Tanggal", "Jam Masuk", "Jam Pulang", "Keterangan", "Terlambat"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(styleHeader);
            }

            // Data rows
            int userRowNum = 1;
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
            for (Absensi absensi : absensiList) {
                Row row = sheet.createRow(rowNum++);
                Cell cell0 = row.createCell(0);
                cell0.setCellValue(userRowNum++);
                cell0.setCellStyle(styleNumber);

                Cell cell1 = row.createCell(1);
                cell1.setCellValue(absensi.getUser().getUsername());
                cell1.setCellStyle(styleNumber);

                Cell cell2 = row.createCell(2);
                cell2.setCellValue(new SimpleDateFormat("yyyy-MM-dd").format(absensi.getTanggalAbsen()));
                cell2.setCellStyle(styleNumber);

                Cell cell3 = row.createCell(3);
                cell3.setCellValue(absensi.getJamMasuk());
                cell3.setCellStyle(styleNumber);

                Cell cell4 = row.createCell(4);
                String jamPulang = absensi.getJamPulang();
                cell4.setCellValue(jamPulang);
                if (jamPulang == null || jamPulang.isEmpty() || jamPulang.equals("-")) {
                    cell4.setCellStyle(styleColorPulangColumn);
                } else {
                    cell4.setCellStyle(styleNumber);
                }

                Cell cell5 = row.createCell(5);
                cell5.setCellValue(absensi.getStatusAbsen());
                cell5.setCellStyle(styleNumber);

                Cell cell6 = row.createCell(6);
                String terlambatString = "";
                if (absensi.getStatusAbsen().equals("Terlambat") && jamPulang != null && !jamPulang.isEmpty() && !jamPulang.equals("-")) {
                    // Menghitung durasi keterlambatan dalam menit, detik, dan milidetik
                    Date shiftStartTime = sdf.parse(absensi.getUser().getShift().getWaktuMasuk());
                    Date actualStartTime = sdf.parse(absensi.getJamMasuk());
                    long terlambatMillis = actualStartTime.getTime() - shiftStartTime.getTime();
                    int totalDetik = (int) (terlambatMillis / 1000);
                    int menit = totalDetik / 60;
                    int detik = totalDetik % 60;
                    int milidetik = (int) (terlambatMillis % 1000);
                    terlambatString = String.format("%02d,%02d,%03d", menit, detik, milidetik);
                    cell6.setCellStyle(styleColorLate); // Set yellow background for lateness
                } else if (absensi.getStatusAbsen().equals("Terlambat")) {
                    cell6.setCellStyle(styleColorLate); // Set yellow background for lateness
                } else {
                    cell6.setCellStyle(styleNumber); // Set default style for other cases
                }
                cell6.setCellValue(terlambatString);
            }
        }

        // Auto-size columns for better readability
        for (int i = 0; i < 7; i++) {
            sheet.autoSizeColumn(i);
        }

        // Write the output to response
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=Absensi_Simpel_" + getMonthName(month) + "_" + year + ".xlsx");
        workbook.write(response.getOutputStream());
        workbook.close();
    }

//    public void excelAbsensiBulananSimpel(int month, int year ,  HttpServletResponse response) throws IOException {
//        Workbook workbook = new XSSFWorkbook();
//        Sheet sheet = workbook.createSheet("Absensi-Simpel");
//
//        Font fontWhite = workbook.createFont();
//        fontWhite.setColor(IndexedColors.WHITE.getIndex());
//
//        // Cell styles
//        CellStyle styleHeader = workbook.createCellStyle();
//        styleHeader.setAlignment(HorizontalAlignment.CENTER);
//        styleHeader.setVerticalAlignment(VerticalAlignment.CENTER);
//        styleHeader.setBorderTop(BorderStyle.THIN);
//        styleHeader.setBorderRight(BorderStyle.THIN);
//        styleHeader.setBorderBottom(BorderStyle.THIN);
//        styleHeader.setBorderLeft(BorderStyle.THIN);
//
//        CellStyle styleTitle = workbook.createCellStyle();
//        styleTitle.setAlignment(HorizontalAlignment.CENTER);
//        styleTitle.setVerticalAlignment(VerticalAlignment.CENTER);
//        Font titleFont = workbook.createFont();
//        titleFont.setBold(true);
//        styleTitle.setFont(titleFont);
//
//        CellStyle styleNumber = workbook.createCellStyle();
//        styleNumber.setAlignment(HorizontalAlignment.CENTER);
//        styleNumber.setVerticalAlignment(VerticalAlignment.CENTER);
//        styleNumber.setBorderTop(BorderStyle.THIN);
//        styleNumber.setBorderRight(BorderStyle.THIN);
//        styleNumber.setBorderBottom(BorderStyle.THIN);
//        styleNumber.setBorderLeft(BorderStyle.THIN);
//        styleNumber.setFont(fontWhite);
//
//        CellStyle styleCenterNumber = workbook.createCellStyle();
//        styleCenterNumber.setAlignment(HorizontalAlignment.CENTER);
//        styleCenterNumber.setVerticalAlignment(VerticalAlignment.CENTER);
//        styleCenterNumber.setBorderTop(BorderStyle.THIN);
//        styleCenterNumber.setBorderRight(BorderStyle.THIN);
//        styleCenterNumber.setBorderBottom(BorderStyle.THIN);
//        styleCenterNumber.setBorderLeft(BorderStyle.THIN);
//
//        // Conditional formatting colors
//        CellStyle styleColorLate = workbook.createCellStyle();
//        styleColorLate.cloneStyleFrom(styleNumber);
//        styleColorLate.setFillForegroundColor(IndexedColors.RED.getIndex());
//        styleColorLate.setFillPattern(FillPatternType.SOLID_FOREGROUND);
//
//        CellStyle styleColorPermission = workbook.createCellStyle();
//        styleColorPermission.cloneStyleFrom(styleNumber);
//        styleColorPermission.setFillForegroundColor(IndexedColors.DARK_YELLOW.getIndex());
//        styleColorPermission.setFillPattern(FillPatternType.SOLID_FOREGROUND);
//
//        CellStyle styleColorEarly = workbook.createCellStyle();
//        styleColorEarly.cloneStyleFrom(styleNumber);
//        styleColorEarly.setFillForegroundColor(IndexedColors.GREEN.getIndex());
//        styleColorEarly.setFillPattern(FillPatternType.SOLID_FOREGROUND);
//
//        // Fetch data
//        List<Absensi> absensiList = absensiRepository.findByMonthAndYear(month , year);
//
//        if (absensiList.isEmpty()) {
//            // Handle case when there are no absences for the given month and year
//            Row emptyRow = sheet.createRow(0);
//            Cell emptyCell = emptyRow.createCell(0);
//            emptyCell.setCellValue("Tidak ada data absensi simpel untuk bulan " + getMonthName(month) + "-" + year );
//            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4)); // Merge cells for message
//        } else {
//            // Group by user
//            Map<String, List<Absensi>> userAbsensiMap = new HashMap<>();
//            for (Absensi absensi : absensiList) {
//                userAbsensiMap.computeIfAbsent(absensi.getUser().getUsername(), k -> new ArrayList<>()).add(absensi);
//            }
//
//            int rowNum = 0;
//
//            // Title row
//            Row titleRow = sheet.createRow(rowNum++);
//            Cell titleCell = titleRow.createCell(0);
//            titleCell.setCellValue("DATA ABSENSI SIMPEL : " + getMonthName(month) );
//            titleCell.setCellStyle(styleTitle);
//            sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 4)); // Merging cells for title
//            rowNum++;
//
//            for (Map.Entry<String, List<Absensi>> entry : userAbsensiMap.entrySet()) {
//                String userName = entry.getKey();
//                List<Absensi> userAbsensi = entry.getValue();
//                String position = userAbsensi.get(0).getUser().getJabatan().getNamaJabatan();
//
//                // Variables to count absences for each user
//                int userTotalLate = 0;
//                int userTotalPermission = 0;
//                int userTotalEarly = 0;
//
//                // Name and Position row
//                Row nameRow = sheet.createRow(rowNum++);
//                Cell nameCell = nameRow.createCell(0);
//                nameCell.setCellValue("Nama :  " + userName);
//                nameCell.setCellStyle(styleHeader);
//
//                Row positionRow = sheet.createRow(rowNum++);
//                Cell positionCell = positionRow.createCell(0);
//                positionCell.setCellValue("Jabatan :   " + position);
//                positionCell.setCellStyle(styleHeader);
//                rowNum++;
//
//                // Header row
//                Row headerRow = sheet.createRow(rowNum++);
//                String[] headers = {"No", "Tanggal Absen", "Jam Masuk", "Jam Pulang", "Keterangan"};
//                for (int i = 0; i < headers.length; i++) {
//                    Cell cell = headerRow.createCell(i);
//                    cell.setCellValue(headers[i]);
//                    cell.setCellStyle(styleHeader);
//                }
//
//                // Data rows
//                int userRowNum = 1;
//                for (Absensi absensi : userAbsensi) {
//                    Row row = sheet.createRow(rowNum++);
//                    Cell cell0 = row.createCell(0);
//                    cell0.setCellValue(userRowNum++);
//                    cell0.setCellStyle(styleCenterNumber); // Use the centered number style
//
//                    Cell cell1 = row.createCell(1);
//                    cell1.setCellValue(new SimpleDateFormat("yyyy-MM-dd").format(absensi.getTanggalAbsen()));
//                    cell1.setCellStyle(styleNumber);
//
//                    Cell cell2 = row.createCell(2);
//                    cell2.setCellValue(absensi.getJamMasuk());
//                    cell2.setCellStyle(styleNumber);
//
//                    Cell cell3 = row.createCell(3);
//                    cell3.setCellValue(absensi.getJamPulang());
//                    cell3.setCellStyle(styleNumber);
//
//                    Cell cell4 = row.createCell(4);
//                    cell4.setCellValue(absensi.getStatusAbsen());
//                    cell4.setCellStyle(styleNumber);
//
//                    CellStyle styleColor = null;
//                    switch (absensi.getStatusAbsen()) {
//                        case "Terlambat":
//                            styleColor = styleColorLate;
//                            userTotalLate++; // Increment late count
//                            break;
//                        case "Izin":
//                            styleColor = styleColorPermission;
//                            userTotalPermission++; // Increment permission count
//                            break;
//                        case "Lebih Awal":
//                            styleColor = styleColorEarly;
//                            userTotalEarly++; // Increment early count
//                            break;
//                    }
//                    if (styleColor != null) {
//                        for (int i = 0; i < headers.length; i++) {
//                            row.getCell(i).setCellStyle(styleColor);
//                        }
//                    }
//                }
//
//                // Adding summary row for each user
//                Row lateRow = sheet.createRow(rowNum++);
//                Cell lateCell = lateRow.createCell(0);
//                lateCell.setCellValue("Terlambat: " + userTotalLate);
//                lateCell.setCellStyle(styleHeader);
//
//                Row permissionRow = sheet.createRow(rowNum++);
//                Cell permissionCell = permissionRow.createCell(0);
//                permissionCell.setCellValue("Izin: " + userTotalPermission);
//                permissionCell.setCellStyle(styleHeader);
//
//                Row earlyRow = sheet.createRow(rowNum++);
//                Cell earlyCell = earlyRow.createCell(0);
//                earlyCell.setCellValue("Lebih Awal: " + userTotalEarly);
//                earlyCell.setCellStyle(styleHeader);
//
//                // Add a blank row between each user's table for readability
//                rowNum++;
//            }
//        }
//
//        for (int i = 0; i < 5; i++) {
//            sheet.autoSizeColumn(i);
//        }
//
//        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
//        response.setHeader("Content-Disposition", "attachment; filename=AbsensiSimpel" + getMonthName(month) + ".xlsx");
//        workbook.write(response.getOutputStream());
//        workbook.close();
//    }


}
