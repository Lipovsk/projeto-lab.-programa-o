package br.edu.unit.chemest;
import br.edu.unit.chemest.service.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SmilesServiceTest {
    final SmilesService service=new SmilesService();
    @Test void ethanol() { assertEquals(3,service.parse("CCO").getAtomCount()); assertTrue(service.depict("CCO").getWidth()>0); }
    @Test void aromatic() { assertEquals(6,service.parse("c1ccccc1").getAtomCount()); assertNotNull(service.depict("c1ccccc1")); }
    @Test void invalid() { assertThrows(InvalidSmilesException.class,()->service.parse("C1(")); }
    @Test void blank() { assertThrows(InvalidSmilesException.class,()->service.parse(" ")); }
}
