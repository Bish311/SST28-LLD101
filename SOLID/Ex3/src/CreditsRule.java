public class CreditsRule implements EligibilityRule {
    @Override
    public String evaluate(StudentProfile s) {
        if (s.earnedCredits < 20) {
            return "credits below 20";
        }
        return null;
    }
}
