lexer grammar aldl;

ID  :	('a'..'z'|'A'..'Z'|'_')* ('a'..'z'|'A'..'Z')
    ;

INT :	'0'..'9'+
    ;

FLOAT
    :   ('0'..'9')+ '.' ('0'..'9')* EXPONENT?
    |   '.' ('0'..'9')+ EXPONENT?
    |   ('0'..'9')+ EXPONENT
    ;

COMMENT
    :   '//' ~('\n'|'\r')* '\r'? '\n' {$channel=HIDDEN;}
    |   '/*' ( options {greedy=false;} : . )* '*/' {$channel=HIDDEN;}
    ;

WS  :   ( ' '
        | '\t'
        | '\r'
        | '\n'
        ) {$channel=HIDDEN;}
    ;

STRING
    :  '"' ( ESC_SEQ | ~('\\'|'"') )* '"'
    ;

CHAR:  '\'' ( ESC_SEQ | ~('\''|'\\') ) '\''
    ;

fragment
EXPONENT : ('e'|'E') ('+'|'-')? ('0'..'9')+ ;

fragment
HEX_DIGIT : ('0'..'9'|'a'..'f'|'A'..'F') ;

fragment
ESC_SEQ
    :   '\\' ('b'|'t'|'n'|'f'|'r'|'\"'|'\''|'\\')
    |   UNICODE_ESC
    |   OCTAL_ESC
    ;

fragment
OCTAL_ESC
    :   '\\' ('0'..'3') ('0'..'7') ('0'..'7')
    |   '\\' ('0'..'7') ('0'..'7')
    |   '\\' ('0'..'7')
    ;

fragment
UNICODE_ESC
    :   '\\' 'u' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
    ;
   
 
FQN_CLASS
	:	(ID '.')* ID
	;

BASE_LAYOUT_TYPE
	:	'linear'
	|	'frame'
	|	'relative'
	|	'table'
	|	'pager'
	|	'grid'
	;
	
BASE_WIDGET_TYPE
	:	'text'
	|	'image'
	|	'button'
	|	'radio'
	|	'check'
	|	'switch'
	|	'toggle'
	|	'progress'
	|	'seek'
	|	'rating'
	|	'spinner'
	|	'web'
	|	'edit'
	;
	
BASE_CONTAINER_TYPE
	:	'list'
	|	'expanable-list'
	|	'grid-view'
	|	'tabhost'
	|	'scroll'
	|	'hscroll'
	;
	
LAYOUT_TYPE
	:	BASE_LAYOUT_TYPE
	|	BASE_WIDGET_TYPE
	|	BASE_CONTAINER_TYPE
	;