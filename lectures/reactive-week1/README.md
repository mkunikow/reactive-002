#Notes
##For expressions

###1. A simple for expression
`for(x <- e1) yield e2 =>  e1.map(x => e2)`

###2. A for expression with filter
`for(x <- e1 if f; s) yield e2 =>  for(x <- e1.withFilter(x => f); s) yield e2`

###3. A for expression flat map
`for(x <- e1; y <- e2; s) yield e3 =>  e1.fatMap(x => for(y <- e2; s) yield e3)`



