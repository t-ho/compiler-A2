package syms;
import machine.StackMachine;
import junit.framework.TestCase;
import source.Position;
import syms.SymEntry.ProcedureEntry;

/**
 * class SymbolTableTest - Junit test for SymbolTable
 * @version $Revision: 22 $  $Date: 2014-05-20 15:14:36 +1000 (Tue, 20 May 2014) $
 */
public class SymbolTableTest extends TestCase {

    public SymbolTableTest(String arg0) {
        super(arg0);
    }
    
    private SymbolTable symtab;
    private ProcedureEntry one;
    private ProcedureEntry two;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        symtab = new SymbolTable();
        ProcedureEntry test = 
            new ProcedureEntry( "test", Position.NO_POSITION, 
                    symtab.getCurrentScope() );
        symtab.newScope( test );
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        symtab = null;
    }
    /* 
     * Test SymbolTable constructor.
     */
    public void testSymbolTable() throws Exception {
        assertEquals( 1, symtab.getCurrentScope().getLevel() );
        assertEquals( Predefined.INTEGER_TYPE, 
                symtab.lookupType( "int" ).getType() );
        assertEquals( Predefined.BOOLEAN_TYPE, 
                symtab.lookupType( "boolean" ).getType() );
        assertEquals( Predefined.BOOLEAN_TYPE,
                symtab.lookupConstant( "true" ).getType() );
        assertEquals( Predefined.BOOLEAN_TYPE,
                symtab.lookupConstant( "false" ).getType() );
        assertEquals( 1, symtab.lookupConstant( "true" ).getValue() );
        assertEquals( 0, symtab.lookupConstant( "false" ).getValue() );
    }
    
    private void checkEntry( SymEntry e, String id, int level, Type type ) {
        assertEquals( id, e.getIdent() );
        assertEquals( level, e.getLevel() );
        assertEquals( type, e.getType() );
    }

    private void checkConstant( SymEntry.ConstantEntry e, String id, int level, 
            Type type, int value ) {
        checkEntry( e, id, level, type );
        assertEquals( value, e.getValue() );
        assertEquals( null, symtab.lookupType( id ) );
        assertEquals( null, symtab.lookupVariable( id ) );
        assertEquals( null, symtab.lookupProcedure( id ) );
    }
    private void checkType( SymEntry.TypeEntry e, String id, int level, Type type ) {
        checkEntry( e, id, level, type );
        assertEquals( null, symtab.lookupConstant( id ) );
        assertEquals( null, symtab.lookupVariable( id ) );
        assertEquals( null, symtab.lookupProcedure( id ) );
    }
    private void checkVariable( SymEntry.VarEntry e, String id, int level, Type type,
            int offset, int varSpace ) {
        checkEntry( e, id, level, type );
        assertEquals( StackMachine.LOCALS_BASE + offset, e.getOffset() );
        assertEquals( varSpace, symtab.getCurrentScope().getVariableSpace() );
        assertEquals( null, symtab.lookupType( id ) );
        assertEquals( null, symtab.lookupConstant( id ) );
        assertEquals( null, symtab.lookupProcedure( id ) );
    }
    private void checkProcedure( SymEntry.ProcedureEntry e, String id, int level ) {
        assertEquals( id, e.getIdent() );
        assertEquals( level, e.getLevel() );
        assertEquals( null, symtab.lookupType( id ) );
        assertEquals( null, symtab.lookupVariable( id ) );
        assertEquals( null, symtab.lookupConstant( id ) );
    }
    /*
     * Test method for 'pl0.symbol_table.SymbolTable.get(String)'
     */
    public void testGet() {
        assertEquals( null, symtab.lookupConstant( "e" ) );
        symtab.addConstant( "e", new Position(0), Predefined.INTEGER_TYPE, 42 );
        SymEntry.ConstantEntry e1 = symtab.lookupConstant( "e" );
        checkConstant( e1, "e", 1, Predefined.INTEGER_TYPE, 42 );

        one = new ProcedureEntry( "one", Position.NO_POSITION, 
                        symtab.getCurrentScope() );
        symtab.newScope( one );
        symtab.addConstant( "e", new Position(0), Predefined.BOOLEAN_TYPE, 0 );
        SymEntry.ConstantEntry e2 = symtab.lookupConstant( "e" );
        checkConstant( e2, "e", 2, Predefined.BOOLEAN_TYPE, 0 );

        two = new ProcedureEntry( "two", Position.NO_POSITION, 
                symtab.getCurrentScope() );
        symtab.newScope( two );
        symtab.addConstant( "e", new Position(0), Predefined.INTEGER_TYPE, 27 );
        SymEntry.ConstantEntry e3 = symtab.lookupConstant( "e" );
        checkConstant( e3, "e", 3, Predefined.INTEGER_TYPE, 27 );
        
        symtab.leaveScope();
        SymEntry.ConstantEntry e4 = symtab.lookupConstant( "e" );
        assertEquals( e2, e4 );
    
        symtab.leaveScope();
        SymEntry.ConstantEntry e5 = symtab.lookupConstant( "e" );
        assertEquals( e1, e5 );
    }

    /*
     * Test method for 'pl0.symbol_table.SymbolTable.enterScope(String)'
     */
    public void testEnterScope() {
        assertEquals( 1, symtab.getCurrentScope().getLevel() );
        ProcedureEntry one = new ProcedureEntry( "one", Position.NO_POSITION, 
                symtab.getCurrentScope() );
        symtab.newScope( one );
        assertEquals( 2, symtab.getCurrentScope().getLevel() );
        two = new ProcedureEntry( "two", Position.NO_POSITION, 
                symtab.getCurrentScope() );
        symtab.newScope( two );
        assertEquals( 3, symtab.getCurrentScope().getLevel() );
    }

    /*
     * Test method for 'pl0.symbol_table.SymbolTable.leaveScope()'
     */
    public void testLeaveScope() {
        ProcedureEntry one = new ProcedureEntry( "one", Position.NO_POSITION, 
                symtab.getCurrentScope() );
        symtab.newScope( one );
        assertEquals( 2, symtab.getCurrentScope().getLevel() );
        two = new ProcedureEntry( "two", Position.NO_POSITION, 
                symtab.getCurrentScope() );
        symtab.newScope( two );
        assertEquals( 3, symtab.getCurrentScope().getLevel() );
        symtab.leaveScope();
        assertEquals( 2, symtab.getCurrentScope().getLevel() );
        symtab.leaveScope();
        assertEquals( 1, symtab.getCurrentScope().getLevel() );
    }

    /*
     * Test method for 'pl0.symbol_table.SymbolTable.getParameterSpace()'
     */
    public void testGetParameterSpace() {

    }

    /*
     * Test method for 'pl0.symbol_table.SymbolTable.addConstant(String, int, Type, int)'
     */
    public void testAddConstant() {
        assertEquals( null, symtab.lookupConstant( "e" ) );
        symtab.addConstant( "e", new Position(0), Predefined.INTEGER_TYPE, 42 );
        SymEntry.ConstantEntry e = symtab.lookupConstant( "e" );
        checkConstant( e, "e", 1, Predefined.INTEGER_TYPE, 42 );
    }

    /*
     * Test method for 'pl0.symbol_table.SymbolTable.addType(String, int, Type)'
     */
    public void testAddType() {
        assertEquals( null, symtab.lookupType( "e" ) );
        symtab.addType( "e", new Position(0), Predefined.INTEGER_TYPE );
        SymEntry.TypeEntry e = symtab.lookupType( "e" );
        checkType( e, "e", 1, Predefined.INTEGER_TYPE );
    }

    /*
     * Test method for 'pl0.symbol_table.SymbolTable.addVariable(String, int, Type)'
     */
    public void testAddVariable() {
        Type.ReferenceType refInt =
            new Type.ReferenceType( 
                    new Type.IdRefType("int",symtab.getCurrentScope(), 
                            new Position(0) ) );
        assertEquals( null, symtab.lookupVariable( "e" ) );
        symtab.addVariable( "e", new Position(0), refInt );
        symtab.resolveCurrentScope();
        SymEntry.VarEntry e = symtab.lookupVariable( "e" );
        checkVariable( e, "e", 1, refInt, 0, 1 );
        
        symtab.addVariable( "f", new Position(0), refInt );
        symtab.resolveCurrentScope();
        SymEntry.VarEntry f = symtab.lookupVariable( "f" );
        checkVariable( f, "f", 1, refInt, 1, 2 );
        
        symtab.addVariable( "g", new Position(0), refInt );
        symtab.resolveCurrentScope();
        SymEntry.VarEntry g = symtab.lookupVariable( "g" );
        checkVariable( g, "g", 1, refInt, 2, 3 );
        
    }

    /*
     * Test method for 'pl0.symbol_table.SymbolTable.addProcedure(String, int)'
     */
    public void testAddProcedure() {
        assertEquals( null, symtab.lookupProcedure( "e" ) );
        symtab.addProcedure( "e", new Position(0) );
        SymEntry.ProcedureEntry e = symtab.lookupProcedure( "e" );
        checkProcedure( e, "e", 1 );
    }

}
