public class CgrRule implements EligibilityRule {
    @Override
    public String evaluate(StudentProfile s) {
        if (s.cgr < 8.0) {
            return "CGR below 8.0";
        }
        return null;
    }
}
