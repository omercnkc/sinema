import { TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { AppComponent } from './app.component';

describe('AppComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RouterTestingModule],
      declarations: [AppComponent],
    }).compileComponents();
  });

  it('should render router outlet inside main content', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const kok: HTMLElement = fixture.nativeElement;
    expect(kok.querySelector('main router-outlet')).not.toBeNull();
  });
});
