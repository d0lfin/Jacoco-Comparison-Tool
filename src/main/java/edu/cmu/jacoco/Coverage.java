package edu.cmu.jacoco;

class Coverage {
    
	int covered;
    int total;
    
    public Coverage() {
    	covered = 0;
    	total = 0;
    }
	
	public Coverage(int covered, int total) {
		this.covered = covered;
		this.total = total;
	}
}