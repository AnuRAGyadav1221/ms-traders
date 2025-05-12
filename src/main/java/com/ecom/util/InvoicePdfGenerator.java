package com.ecom.util;

import com.ecom.model.Product;
import com.ecom.model.ProductOrder;
import com.ecom.model.RefundAudit;
import com.ecom.model.OrderAddress;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;

import java.io.ByteArrayOutputStream;

public class InvoicePdfGenerator {

    public static byte[] generateInvoicePDF(ProductOrder order,RefundAudit refund) {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
            Font headerFont = new Font(Font.HELVETICA, 14, Font.BOLD);
            Font normalFont = new Font(Font.HELVETICA, 12);
            Font boldFont = new Font(Font.HELVETICA, 12, Font.BOLD);

            // Title
            Paragraph title = new Paragraph("Ms SAMYABHI TRADERS Invoice", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));
            document.add(new LineSeparator());
            document.add(new Paragraph(" "));

            // Order Info - Aligned Two Column Layout
            PdfPTable orderInfoTable = new PdfPTable(2);
            orderInfoTable.setWidthPercentage(100);
            orderInfoTable.setWidths(new float[]{1, 1}); // Equal widths

            orderInfoTable.addCell(getCell("Order ID: " + order.getOrderId(), PdfPCell.ALIGN_LEFT, normalFont));
            orderInfoTable.addCell(getCell("Order Date: " + order.getOrderDate(), PdfPCell.ALIGN_RIGHT, normalFont));

            orderInfoTable.addCell(getCell("Payment ID: " + (order.getPayPaymentId() != null ? order.getPayPaymentId() : "N/A"), PdfPCell.ALIGN_LEFT, normalFont));
            orderInfoTable.addCell(getCell("Payment Mode: " + order.getPaymentType(), PdfPCell.ALIGN_RIGHT, normalFont));

            document.add(orderInfoTable);
            document.add(new Paragraph(" "));


            // Address
            OrderAddress address = order.getOrderAddress();
            document.add(new Paragraph("Shipping Address:", boldFont));
            document.add(new Paragraph(" "));
            if (address != null) {

                PdfPTable addressTable = new PdfPTable(1);

                addressTable.addCell(getCell("Name: " + address.getFirstName() + " " + address.getLastName(), PdfPCell.ALIGN_LEFT, normalFont));
                addressTable.addCell(getCell("Address: " + address.getAddress(), PdfPCell.ALIGN_LEFT, normalFont));
                addressTable.addCell(getCell(address.getCity() + ", " + address.getState() + " - " + address.getPincode(), PdfPCell.ALIGN_LEFT, normalFont));
                addressTable.addCell(getCell("Mobile: " + address.getMobileNo(), PdfPCell.ALIGN_LEFT, normalFont));
                addressTable.addCell(getCell("Email: " + address.getEmail(), PdfPCell.ALIGN_LEFT, normalFont));

                document.add(addressTable);

            } else {
                document.add(new Paragraph("N/A", normalFont));
            }
            document.add(new Paragraph(" "));

            // Product Table
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new int[]{4, 1, 2, 2});

            // Table Headers
            PdfPCell cell;
            cell = new PdfPCell(new Phrase("Product Name", boldFont));
            table.addCell(cell);
            table.addCell(new PdfPCell(new Phrase("Qty", boldFont)));
            table.addCell(new PdfPCell(new Phrase("Unit Price", boldFont)));
            table.addCell(new PdfPCell(new Phrase("Total Price", boldFont)));

            // Product Row
            Product product = order.getProduct();
            table.addCell(new PdfPCell(new Phrase(product.getTitle(), normalFont)));
            table.addCell(new PdfPCell(new Phrase(order.getQuantity().toString(), normalFont)));
            table.addCell(new PdfPCell(new Phrase("₹" + order.getPrice(), normalFont)));
            double totalPrice = order.getQuantity() * order.getPrice();
            table.addCell(new PdfPCell(new Phrase("₹" + totalPrice, normalFont)));

            document.add(table);

            document.add(new Paragraph(" "));

            PdfPTable summary = new PdfPTable(2);
            summary.setWidthPercentage(50);
            summary.setHorizontalAlignment(Element.ALIGN_RIGHT);
            summary.setSpacingBefore(20f);


            summary.addCell(new Phrase("Subtotal:", boldFont));
            summary.addCell(new Phrase("₹" + totalPrice, normalFont));
            summary.addCell(new Phrase("Tax: ", boldFont));
            summary.addCell(new Phrase("₹" + order.getTax(), normalFont));
            summary.addCell(new Phrase("Delivery Fee:", boldFont));
            summary.addCell(new Phrase("₹" + order.getDeliveryFee(), normalFont));
            summary.addCell(new Phrase("Grand Total:", boldFont));
            summary.addCell(new Phrase("₹" + order.getGrandTotal(), boldFont));

            document.add(summary);
            if (refund != null) {
                document.add(new Paragraph(" "));
                document.add(new Paragraph("Refund Details:", boldFont));
                document.add(new Paragraph(" "));

                PdfPTable refundTable = new PdfPTable(2);
                refundTable.setWidthPercentage(50);
                refundTable.setHorizontalAlignment(Element.ALIGN_LEFT);

                refundTable.addCell(getCell("Refund ID:", PdfPCell.ALIGN_LEFT, normalFont));
                refundTable.addCell(getCell(refund.getRazorpayRefundId(), PdfPCell.ALIGN_LEFT, normalFont));

                refundTable.addCell(getCell("Refund Amount:", PdfPCell.ALIGN_LEFT, normalFont));
                refundTable.addCell(getCell("₹" + refund.getRefundAmount(), PdfPCell.ALIGN_LEFT, normalFont));

                refundTable.addCell(getCell("Refund Time:", PdfPCell.ALIGN_LEFT, normalFont));
                refundTable.addCell(getCell(refund.getRefundTime().toString(), PdfPCell.ALIGN_LEFT, normalFont));

                document.add(refundTable);
            }

            document.add(new Paragraph(" "));

            Paragraph thankYou = new Paragraph(
            "Thank you for shopping with us!\n\n" +
            "We truly appreciate your business and trust in our service.\n" +
            "If you have any questions or need support, feel free to reach out to our team.\n\n" +
            "We look forward to serving you again!\n\n" +
            "Warm regards,\n" +
            "The Ms SAMYABHI TRADERS Team", headerFont);
            thankYou.setAlignment(Element.ALIGN_CENTER);
            thankYou.setSpacingBefore(30f);
            document.add(thankYou);

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return out.toByteArray();
    }

    private static PdfPCell getCell(String text, int alignment, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(alignment);
        return cell;
    }
    
}
