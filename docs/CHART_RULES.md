# Chart implementation

The supplied course PDFs are preserved in `docs/references/`. No unrelated BMI standard is substituted.

## Adults: age > 18

Source: `BMI_chart_adult.pdf`, Hong Kong Centre for Health Protection, Body Mass Index Chart. The chart describes Asian adult BMI and excludes pregnancy. Gender does not alter its cutoffs.

| Category | BMI |
| --- | --- |
| Underweight | < 18.5 |
| Normal range | >= 18.5 and < 23 |
| Overweight | >= 23 and < 25 |
| Obese | >= 25 |

The poster prints normal as 18.5-22.9 and overweight as 23-24.9. The app uses the continuous intervals above, with no rounding before adult classification. Decimal arithmetic preserves exact boundaries; the displayed BMI has two decimal places.

The adult PDF does not define separate severe-underweight or severe-overweight categories. The app preserves its four names. The obese category receives a red attention card; underweight and overweight receive amber cards.

## Children: ages 6-18, inclusive

Source: `BMI_chart_child.pdf`, Health Promotion Board, Body Mass Index (BMI)-for-Age, Children and Youth Aged 6 to 18 Years Old.

Use the boy/girl table according to the selected gender and the row for completed age in years. All 26 rows are transcribed into `BmiReference.kt`. Categories are severely underweight, underweight, acceptable weight, overweight, and severely overweight.

The PDF prints adjacent intervals to one decimal (for example, <=12.8 followed by 12.9-13.1 for boys aged 6). To avoid unclassified gaps, the app explicitly rounds child BMI to one decimal using HALF_UP before table lookup. This is an implementation convention, disclosed in the report and chart dialog; the PDF does not state a rounding rule. A BMI of 12.849 becomes 12.8; 12.85 becomes 12.9. No interpolation between age rows is used.

Both severe child categories receive a red border, pale-red panel, warning symbol, written attention message and advice to discuss the result with a parent/caregiver and a healthcare professional. Normal/acceptable results use green, other out-of-range results use amber. Meaning is conveyed through text as well as colour.

Ages 1-5 still receive the calculated BMI, but the category is explicitly unavailable because the supplied chart starts at age 6. The app does not extrapolate child thresholds or apply adult thresholds below age 19.

## Audit and checks

`app/src/test/resources/child-chart.csv` is a separate transcription of the published endpoints. The test suite checks all 208 endpoints across 26 rows, adult boundaries, child rounding, gender differences, the age 18/19 switch, under-6 handling and severe warnings. On-device tests exercise the actual form/report navigation, saved inputs, chart dialog and landscape controls.
