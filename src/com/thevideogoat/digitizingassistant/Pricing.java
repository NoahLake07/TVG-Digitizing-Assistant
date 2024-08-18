package com.thevideogoat.digitizingassistant;

import com.thevideogoat.digitizingassistant.data.Conversion;
import com.thevideogoat.digitizingassistant.data.Project;
import com.thevideogoat.digitizingassistant.data.Type;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Pricing {

    /**
        Pricing Model for Tape Conversion Services:

            VHS:
        Suggested Price: $25 - $30 per tape.
        - Standard rate for up to 2 hours of footage.
        - Additional hours: $10 per hour.

            VHS-C:
        Suggested Price: $25 - $30 per tape.
        - Priced similarly to VHS due to similar conversion processes.

            8mm:
        Suggested Price: $30 - $35 per tape.
        - Higher rate due to specialized equipment and extra care needed for older tapes.

            Betamax:
        Suggested Price: $40 - $50 per tape.
        - Higher rate justified by the rarity of the format and the costly equipment required.

            MiniDV:
        Suggested Price: $30 - $35 per tape.
        - Digital format; requires careful handling, justifying a slightly higher price.

            CD/DVD Duplication:
        Suggested Price: $10 - $15 per disc.
        - Covers duplication, disc, case, and labeling costs.

            Bulk Discounts:
        - 10% discount for orders over 10 tapes.
        - 15% discount for orders over 20 tapes.

            Additional Charges:
        - Extra Hours: $10 per hour for tapes exceeding 2 hours.
        - Editing Services: $10 - $15 per hour, depending on complexity.
     */


    // Conversion Pricing
    public static final double PRICE_VHS = 30.00; // USD per tape
    public static final double PRICE_VHSC = 30.00; // USD per tape
    public static final double PRICE_8MM = 35.00; // USD per tape
    public static final double PRICE_BETAMAX = 45.00; // USD per tape
    public static final double PRICE_MINIDV = 35.00; // USD per tape
    public static final double PRICE_CD_DVD = 12.00; // USD per disc
    public static final double PRICE_PER_ADDITIONAL_HOUR = 10.00; // USD
    public static final double PRICE_PER_BASIC_EDITED_TAPE = 10.00; // USD

    // Tape Length
    public static final double STANDARD_TAPE_LENGTH = 120.0; // min

    // Bulk Discount
    public static final int bulkDiscountThreshold = 10;
    public static final double bulkDiscountMax = 0.15;
    public static final int bulkDiscountSetSize = 10;
    public static final double bulkDiscountPerSet = 0.05;

    // Minimum Price
    public static final double MINIMUM_PROJECT_PRICE = 20.00; // USD

    // Tape Format Pricing
    public static final Map<Type, Double> tapeFormatPricing;

    static {
        tapeFormatPricing = new HashMap<>();
        tapeFormatPricing.put(Type.VHS, PRICE_VHS);
        tapeFormatPricing.put(Type.VHSC, PRICE_VHSC);
        tapeFormatPricing.put(Type._8MM, PRICE_8MM);
        tapeFormatPricing.put(Type.BETAMAX, PRICE_BETAMAX);
        tapeFormatPricing.put(Type.MINIDV, PRICE_MINIDV);
        tapeFormatPricing.put(Type.CD_DVD, PRICE_CD_DVD);
    }

    public double calculateProjectCost(Project project) {
        List<Map<String, String>> breakdown = new ArrayList<>();
        Map<String, String> header = new HashMap<>();
        header.put("Description", "Cost");
        breakdown.add(header);

        // GATHER DETAILS OF TAPE(S)
        ArrayList<TapeInfo> tapes = new ArrayList<>();
        for (Conversion c : project.getConversions()) {
            TapeInfo tapeInfo = new TapeInfo(c);
            tapes.add(tapeInfo);
            breakdown.addAll(tapeInfo.getBreakdown());
        }

        // SUM COSTS
        double totalCost = 0.00;
        for (TapeInfo t : tapes) {
            double tapeCost = Math.max(t.price, MINIMUM_PROJECT_PRICE);
            Map<String, String> tapeCostEntry = new HashMap<>();
            tapeCostEntry.put("Description", "Tape Cost (" + t.type + ")");
            tapeCostEntry.put("Cost", "$" + tapeCost);
            breakdown.add(tapeCostEntry);
            totalCost += tapeCost;
        }

        // BULK DISCOUNT
        if (tapes.size() >= bulkDiscountThreshold) {
            double discount = 0.00;
            for (int i = bulkDiscountThreshold; i < tapes.size(); i+= bulkDiscountSetSize) {
                discount += bulkDiscountPerSet; // percentage discount
            }
            discount = Math.min(discount, bulkDiscountMax);
            double discountAmount = totalCost * discount;
            totalCost -= totalCost * discount;
            Map<String, String> discountEntry = new HashMap<>();
            discountEntry.put("Description", "Bulk Discount (" + (discount * 100) + "%)");
            discountEntry.put("Cost", "-$" + discountAmount);
            breakdown.add(discountEntry);
        }

        Map<String, String> totalCostEntry = new HashMap<>();
        totalCostEntry.put("Description", "Total Cost");
        totalCostEntry.put("Cost", "$" + totalCost);
        breakdown.add(totalCostEntry);

        printBreakdown(breakdown);

        return totalCost;
    }

    private void printBreakdown(List<Map<String, String>> breakdown) {
        System.out.println(String.format("%-30s %10s", "Description", "Cost"));
        System.out.println("--------------------------------------------------");
        for (Map<String, String> entry : breakdown) {
            System.out.println(String.format("%-30s %10s", entry.get("Description"), entry.get("Cost")));
        }
    }

    class TapeInfo {
        Type type;
        Duration duration;
        double price;
        List<Map<String, String>> breakdown;
        String name, notes;

        public TapeInfo(Conversion c) {
            this.name = c.name;
            this.notes = c.note;
            this.type = c.type;
            this.duration = c.duration;
            this.breakdown = new ArrayList<>();
            calculatePrice();
        }

        public TapeInfo(Type type, Duration duration) {
            this.type = type;
            this.duration = duration;
            this.breakdown = new ArrayList<>();
            calculatePrice();
        }

        /**
         * @return a double[] containing cost info for each tape
         *     <p>- 0: base price for tape</p>
         *     <p>- 1: additional charge for extra hours</p>
         *     <p>- 2: total price for tape</p>
         */
        protected double[] calculatePrice() {

            double[] var = new double[2];

            if (type != null && duration != null) {
                // BASE PRICE
                double basePrice = 0.00;
                switch (type) {
                    case CD_DVD -> basePrice = PRICE_CD_DVD;
                    case VHS -> basePrice = PRICE_VHS;
                    case VHSC -> basePrice = PRICE_VHSC;
                    case _8MM -> basePrice = PRICE_8MM;
                    case BETAMAX -> basePrice = PRICE_BETAMAX;
                    case MINIDV -> basePrice = PRICE_MINIDV;
                }
                price += basePrice;
                var[0] = basePrice;

                Map<String, String> basePriceEntry = new HashMap<>();
                basePriceEntry.put("Description", "Base Price for " + type);
                basePriceEntry.put("Cost", "$" + basePrice);
                breakdown.add(basePriceEntry);

                // TAPE DURATION
                if (duration.toMinutes() > STANDARD_TAPE_LENGTH) {
                    double durationCost = 0.00;
                    for (double i = STANDARD_TAPE_LENGTH; i < duration.toMinutes(); i += 60.00) {
                        durationCost += PRICE_PER_ADDITIONAL_HOUR;
                        price += PRICE_PER_ADDITIONAL_HOUR;

                        Map<String, String> additionalHourEntry = new HashMap<>();
                        additionalHourEntry.put("Description", "Additional Hour Charge");
                        additionalHourEntry.put("Cost", "$" + PRICE_PER_ADDITIONAL_HOUR);
                        breakdown.add(additionalHourEntry);
                    }
                    var[1] = durationCost;
                } else {
                    price = basePrice;
                    var[1] = basePrice;
                }
            }

            return var;
        }

        public List<Map<String, String>> getBreakdown() {
            return breakdown;
        }
    }

    public static void main(String[] args) {
        Project testProject = new Project("Test Project");
        addConversionToProject(Type.VHS, 34, testProject);
        addConversionToProject(Type.VHSC, 1, testProject);
        addConversionToProject(Type._8MM, 15, testProject);
        addConversionToProject(Type.BETAMAX, 0, testProject);
        addConversionToProject(Type.MINIDV, 8, testProject);
        addConversionToProject(Type.CD_DVD, 68, testProject);

        Pricing pricing = new Pricing();
        pricing.calculateProjectCost(testProject);
    }

    private static void addConversionToProject(Type type, int qty, Project project){
        for (int i = 0; i < qty; i++) {
            Conversion c = new Conversion(type.toString()+" "+ i);
            c. duration = Duration.ofMinutes(120);
            c. type = type;
            project.addConversion(c);
        }
    }

    class Quote {
        String projectName;
        ArrayList<TapeInfo> tapes;
        double totalCost;

        HashMap<Type,QtyPrice> tapeData;

        public Quote(String projectName, ArrayList<TapeInfo> tapes) {
            this.projectName = projectName;
            this.tapes = tapes;

            totalCost = 0.00;
            for (TapeInfo t : tapes) {
                totalCost += t.price;
            }

            tapeData = new HashMap<>();
            for (TapeInfo t : tapes) {
                if (tapeData.containsKey(t.type)) {
                    tapeData.get(t.type).qty++;
                } else {

                }
            }
        }

        public String toString(){
            StringBuilder sb = new StringBuilder();
            sb.append("Project Quote: " + projectName);
            sb.append("==============================\n");
            sb.append("Name\tQty\tEst. Price");
            for(Type type : Type.values()){
                if(tapeData.get(type) != null){
                    QtyPrice data = tapeData.get(type);
                    sb.append(type.toString() + "\t" + data.qty +"\t"+ data.price);
                }
            }

            return sb.toString();
        }
    }

    class QtyPrice{
        int qty;
        int price;

        public QtyPrice(int qty, int price){
            this.qty = qty;
            this.price = price;
        }
    }

}