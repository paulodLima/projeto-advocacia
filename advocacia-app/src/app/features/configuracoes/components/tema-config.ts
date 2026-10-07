import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TEMAS } from '../../../core/theme/tema.model';
import { TemaService } from '../../../core/theme/tema.service';
import { Icone } from '../../../shared/components/icone/icone';
@Component({ selector: 'app-tema-config', imports: [FormsModule, RouterLink, Icone], templateUrl: './tema-config.html', styleUrl: './tema-config.scss' })
export class TemaConfig { protected readonly tema = inject(TemaService); protected readonly temas = TEMAS; }
