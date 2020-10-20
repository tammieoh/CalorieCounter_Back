package app;

public class CalorieCounter {
    // instance variables
    double goal;
    double weight;
    double percentage;
    String type;
    String sex;
    // constructor
    public CalorieCounter(double weight, double percentage, String type, String sex) {
        weight = this.weight;
        percentage = this.percentage;
        type = this.type;
        sex = this.sex;

    }
    public int createCounter(double weight, double percentage, String type, String sex) {
        goal = 0;
        int calories = 0;
        if(sex.equals("female")){
            if(type.equals("lose")) {
                if(percentage == 5) {
                    goal = weight - (weight * (percentage*0.01));
                    calories = 1500;
                }
                else {
                    goal = weight - (weight * (percentage*0.01));
                    calories = 1500;
                }
            }
            else {
                if(percentage == 5) {
                    goal = weight + (weight * (percentage*0.01));
                    calories = 2500;
                }
                else {
                    goal = weight + (weight * (percentage*0.01));
                    calories = 2500;
                }
            }
        }
        else {
            if(type.equals("lose")) {
                if(percentage == 5) {
                    goal = weight - (weight * (percentage*0.01));
                    calories = 2000;
                }
                else {
                    goal = weight - (weight * (percentage*0.01));
                    calories = 2000;
                }
            }
            else {
                if(percentage == 5) {
                    goal = weight + (weight * (percentage*0.01));
                    calories = 3000;
                }
                else {
                    goal = weight + (weight * (percentage*0.01));
                    calories = 3000;
                }
            }
        }
        return calories;
    }
    // methods
//    public int addCalories()
}
