package org.codehaus.groovy.control;

import org.codehaus.groovy.control.ProcessingUnit;
import org.codehaus.groovy.control.SourceUnit;

public privileged aspect LitGroovy {

	pointcut nextphase() :
        call(public void ProcessingUnit.nextPhase())
        && withincode(public void SourceUnit.parse());

	before() : nextphase() {
		System.out.println("test");
	}

}
